package com.divudi.service;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.OpdIncomeReportDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import org.junit.jupiter.api.Test;

import javax.persistence.TemporalType;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BillServiceOpdIncomeReportDtoTest {

    private static class DummyBillFacade extends BillFacade {
        String jpql;
        Map<String, Object> params;
        @Override
        public List<?> findLightsByJpql(String jpql, java.util.Map<String, Object> parameters, TemporalType tt) {
            this.jpql = jpql;
            this.params = new HashMap<>(parameters);
            return Collections.emptyList();
        }
    }

    @Test
    public void verifyQueryAndParamsPassedToFacade() throws Exception {
        BillService service = new BillService();
        DummyBillFacade facade = new DummyBillFacade();
        Field f = BillService.class.getDeclaredField("billFacade");
        f.setAccessible(true);
        f.set(service, facade);

        Institution institution = new Institution();
        institution.setId(1L);
        Institution site = new Institution();
        site.setId(2L);
        Department dep = new Department();
        dep.setId(3L);
        WebUser user = new WebUser();

        Date from = new Date(0);
        Date to = new Date();
        List<BillTypeAtomic> bts = Collections.singletonList(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);

        service.fetchOpdIncomeReportDTOs(from, to, institution, site, dep, user, bts, null, null);

        assertNotNull(facade.jpql);
        assertTrue(facade.jpql.contains("b.createdAt between :fromDate and :toDate"));
        assertTrue(facade.jpql.contains("b.billTypeAtomic in :billTypesAtomics"));

        assertEquals(from, facade.params.get("fromDate"));
        assertEquals(to, facade.params.get("toDate"));
        assertEquals(institution, facade.params.get("ins"));
        assertEquals(site, facade.params.get("site"));
        assertEquals(dep, facade.params.get("dep"));
        assertEquals(user, facade.params.get("user"));
    }

    @Test
    public void testNullParameterValidation() {
        BillService service = new BillService();

        assertThrows(IllegalArgumentException.class, () -> {
            service.fetchOpdIncomeReportDTOs(null, new Date(), null, null, null, null, new ArrayList<>(), null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            service.fetchOpdIncomeReportDTOs(new Date(), null, null, null, null, null, new ArrayList<>(), null, null);
        });

        Date from = new Date(System.currentTimeMillis() + 86400000);
        Date to = new Date();
        assertThrows(IllegalArgumentException.class, () -> {
            service.fetchOpdIncomeReportDTOs(from, to, null, null, null, null, new ArrayList<>(), null, null);
        });
    }
}
