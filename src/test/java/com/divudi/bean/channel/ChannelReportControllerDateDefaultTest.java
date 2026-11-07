package com.divudi.bean.channel;

import com.divudi.core.entity.Bill;
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
 * A cleared report date must mean today on the first click (#25224, as #25085):
 * the query has to bind the same default that getFromDate()/getToDate() show.
 */
public class ChannelReportControllerDateDefaultTest {

    private static class CapturingBillFacade extends BillFacade {
        Map<String, Object> params;

        @Override
        public List<Bill> findByJpql(String jpql, Map<String, Object> parameters, TemporalType tt) {
            this.params = new HashMap<>(parameters);
            return Collections.emptyList();
        }
    }

    private CapturingBillFacade injectFacade(ChannelReportController controller) throws Exception {
        CapturingBillFacade facade = new CapturingBillFacade();
        Field f = ChannelReportController.class.getDeclaredField("billFacade");
        f.setAccessible(true);
        f.set(controller, facade);
        return facade;
    }

    @Test
    public void clearedDates_bindTodayOnFirstClick() throws Exception {
        ChannelReportController controller = new ChannelReportController();
        CapturingBillFacade facade = injectFacade(controller);
        controller.setFromDate(null);
        controller.setToDate(null);

        controller.listShiftStartBills();

        Date fd = (Date) facade.params.get("fd");
        Date td = (Date) facade.params.get("td");
        assertNotNull(fd);
        assertNotNull(td);
        // Each bound value is checked against its own day, so a run across midnight still passes.
        assertEquals(CommonFunctions.getStartOfDay(fd), fd);
        assertEquals(CommonFunctions.getEndOfDay(td), td);
        assertTrue(td.after(fd));
        long now = System.currentTimeMillis();
        assertTrue(fd.getTime() <= now && now - fd.getTime() < 2L * 24 * 60 * 60 * 1000);
        assertSame(controller.getFromDate(), fd);
        assertSame(controller.getToDate(), td);
    }

    @Test
    public void chosenDates_areBoundUnchanged() throws Exception {
        ChannelReportController controller = new ChannelReportController();
        CapturingBillFacade facade = injectFacade(controller);
        Date from = new Date(1_700_000_000_000L);
        Date to = new Date(1_700_086_400_000L);
        controller.setFromDate(from);
        controller.setToDate(to);

        controller.listShiftStartBills();

        assertSame(from, facade.params.get("fd"));
        assertSame(to, facade.params.get("td"));
    }
}
