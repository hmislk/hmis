package com.divudi.bean.channel;

import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.CommonFunctions;
import org.junit.jupiter.api.Test;

import javax.persistence.TemporalType;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A cleared report date must mean today on the first Process click (#25085):
 * the query has to bind the same default that getFromDate()/getToDate() show.
 */
public class ChannelReportTemplateControllerDateDefaultTest {

    private static class CapturingBillFacade extends BillFacade {
        Map<String, Object> params;

        @Override
        public List<?> findLightsByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
            this.params = new HashMap<>(parameters);
            return Collections.emptyList();
        }
    }

    private CapturingBillFacade injectFacade(ChannelReportTemplateController controller) throws Exception {
        CapturingBillFacade facade = new CapturingBillFacade();
        Field f = ChannelReportTemplateController.class.getDeclaredField("billFacade");
        f.setAccessible(true);
        f.set(controller, facade);
        return facade;
    }

    @Test
    public void clearedDates_bindTodayOnFirstProcess() throws Exception {
        ChannelReportTemplateController controller = new ChannelReportTemplateController();
        CapturingBillFacade facade = injectFacade(controller);
        controller.setFromDate(null);
        controller.setToDate(null);

        controller.fillDailySessionCounts();

        Date fd = (Date) facade.params.get("fd");
        Date td = (Date) facade.params.get("td");
        assertNotNull(fd);
        assertNotNull(td);
        // Compare with the bound values themselves so the test cannot fail across midnight.
        assertEquals(CommonFunctions.getStartOfDay(fd), fd);
        assertEquals(CommonFunctions.getEndOfDay(fd), td);
        long now = System.currentTimeMillis();
        assertTrue(fd.getTime() <= now && now - fd.getTime() < 2L * 24 * 60 * 60 * 1000);
        assertSame(controller.getFromDate(), fd);
        assertSame(controller.getToDate(), td);
    }

    @Test
    public void chosenDates_areBoundUnchanged() throws Exception {
        ChannelReportTemplateController controller = new ChannelReportTemplateController();
        CapturingBillFacade facade = injectFacade(controller);
        Date from = new Date(1_700_000_000_000L);
        Date to = new Date(1_700_086_400_000L);
        controller.setFromDate(from);
        controller.setToDate(to);

        controller.fillCategorySessionCounts();

        assertSame(from, facade.params.get("fd"));
        assertSame(to, facade.params.get("td"));
    }
}
