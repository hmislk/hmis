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

        // Test with realistic entity parameters
        Institution fromInstitution = new Institution();
        fromInstitution.setId(1L);
        Institution toInstitution = new Institution(); 
        toInstitution.setId(2L);
        Department department = new Department();
        department.setId(3L);

        Date from = new Date(0);
        Date to = new Date();
        service.fetchLabDailySummaryDtos(from, to, fromInstitution, toInstitution, department);

        assertNotNull(facade.jpql);
        assertTrue(facade.jpql.contains("group by b.paymentMethod"));
        // Verify additional expected query elements
        assertTrue(facade.jpql.contains("type(i) = com.divudi.core.entity.lab.Investigation"));
        assertTrue(facade.jpql.contains("b.createdAt between :fromDate and :toDate"));
        
        assertEquals(from, facade.params.get("fromDate"));
        assertEquals(to, facade.params.get("toDate"));
        assertEquals(fromInstitution, facade.params.get("ins"));
        assertEquals(toInstitution, facade.params.get("site"));
        assertEquals(department, facade.params.get("dep"));
    }

    @Test
    public void testNullParameterValidation() {
        BillService service = new BillService();
        
        // Test null fromDate
        assertThrows(IllegalArgumentException.class, () -> {
            service.fetchLabDailySummaryDtos(null, new Date(), null, null, null);
        });
        
        // Test null toDate
        assertThrows(IllegalArgumentException.class, () -> {
            service.fetchLabDailySummaryDtos(new Date(), null, null, null, null);
        });
        
        // Test fromDate after toDate
        Date from = new Date(System.currentTimeMillis() + 86400000); // tomorrow
        Date to = new Date(); // today
        assertThrows(IllegalArgumentException.class, () -> {
            service.fetchLabDailySummaryDtos(from, to, null, null, null);
        });
    }
}
