package com.divudi.ejb;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.StockFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OptimizedPharmacyBeanTest {

    private static class MockStockFacade extends StockFacade {
        private Map<Long, Stock> map = new HashMap<>();
        void addStock(Stock s) { map.put(s.getId(), s); }
        @Override
        public Stock findWithoutCache(Object id) { return map.get(id); }
        @Override
        public void batchEdit(List<Stock> stocks) { for(Stock s:stocks) map.put(s.getId(), s); }
        @Override
        public void edit(Stock entity) { map.put(entity.getId(), entity); }
    }

    @Test
    public void deductFromStockBatch_updatesStock() {
        OptimizedPharmacyBean bean = new OptimizedPharmacyBean();
        MockStockFacade sf = new MockStockFacade();
        bean.setStockFacade(sf);
        bean.setPharmacyBean(null);

        Stock st = new Stock();
        st.setId(1L);
        st.setStock(10.0);
        sf.addStock(st);

        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        Department dept = new Department();
        dept.setInstitution(new Institution());
        OptimizedPharmacyBean.StockDeductionRequest req = new OptimizedPharmacyBean.StockDeductionRequest(st, 2.0, pbi, dept);
        List<OptimizedPharmacyBean.StockDeductionRequest> list = new ArrayList<>();
        list.add(req);

        boolean result = bean.deductFromStockBatch(list);
        assertTrue(result);
        assertEquals(8.0, st.getStock(), 0.001);
    }
}
