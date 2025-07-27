package com.divudi.service;

import com.divudi.core.data.dto.LabDailySummaryDTO;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import org.junit.jupiter.api.Test;

import javax.persistence.TemporalType;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BillServiceLabSummaryTest {

    private static class DummyBillItemFacade extends BillItemFacade {
        String jpql;
        HashMap<String, Object> params;
        @Override
        public List<?> findLightsByJpql(String jpql, java.util.Map<String, Object> parameters, TemporalType tt) {
            this.jpql = jpql;
            this.params = new HashMap<>(parameters);
            return java.util.Collections.emptyList();
        }
    }

    @Test
    public void verifyQueryAndParamsPassedToFacade() throws Exception {
        BillService service = new BillService();
        DummyBillItemFacade facade = new DummyBillItemFacade();
        Field f = BillService.class.getDeclaredField("billItemFacade");
        f.setAccessible(true);
        f.set(service, facade);

        Date from = new Date(0);
        Date to = new Date();
        service.fetchLabDailySummaryDtos(from, to, new Institution(), new Institution(), new Department());

        assertNotNull(facade.jpql);
        assertTrue(facade.jpql.contains("group by b.paymentMethod"));
        assertEquals(from, facade.params.get("fromDate"));
        assertEquals(to, facade.params.get("toDate"));
    }
}
