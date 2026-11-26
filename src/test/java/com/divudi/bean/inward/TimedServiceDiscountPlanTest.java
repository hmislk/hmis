package com.divudi.bean.inward;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The timed-service discount step of Create Final Bill gives the same figures as the old per-row loop (#17562). */
class TimedServiceDiscountPlanTest {

    private static Object[] row(long id, double gross, double margin, Date at) {
        return new Object[]{id, gross, margin, at};
    }

    @Test
    void totalAndPerRowDiscountMatchTheOldFormula() {
        List<Object[]> rows = Arrays.asList(
                row(1, 1234.57, 12.3, new Date(1000)),
                row(2, 99.99, 0.0, new Date(2000)),
                row(3, 0.1, 0.2, new Date(3000)));
        double pct = 7.5;
        double expectedTotal = 0;
        for (Object[] r : rows) {
            double value = (Double) r[1] + (Double) r[2];
            expectedTotal += (value * pct) / 100;
        }
        BhtSummeryController.TimedDiscountPlan plan = BhtSummeryController.planTimedServiceDiscount(rows, pct, null, null);
        assertEquals(Arrays.asList(1L, 2L, 3L), plan.ids);
        assertEquals(Double.doubleToLongBits(expectedTotal), Double.doubleToLongBits(plan.total));
    }

    @Test
    void windowKeepsOnlyRowsInsideItAndNullTimeOnlyInOpenEnded() {
        List<Object[]> rows = Arrays.asList(
                row(1, 100, 0, new Date(1000)),
                row(2, 100, 0, new Date(5000)),
                row(3, 100, 0, null));
        BhtSummeryController.TimedDiscountPlan closed = BhtSummeryController.planTimedServiceDiscount(rows, 10, new Date(0), new Date(2000));
        assertEquals(Arrays.asList(1L), closed.ids);
        BhtSummeryController.TimedDiscountPlan open = BhtSummeryController.planTimedServiceDiscount(rows, 10, new Date(2000), null);
        assertEquals(Arrays.asList(2L, 3L), open.ids);
        assertEquals(20.0, open.total, 0.0);
    }

    @Test
    void rowsWithTheSameDiscountShareOneMirrorUpdate() {
        List<Object[]> rows = Arrays.asList(
                row(1, 200, 0, null), row(2, 100, 0, null), row(3, 200, 0, null));
        BhtSummeryController.TimedDiscountPlan plan = BhtSummeryController.planTimedServiceDiscount(rows, 10, null, null);
        assertEquals(2, plan.idsByDiscount.size());
        assertEquals(Arrays.asList(1L, 3L), plan.idsByDiscount.get(20.0));
        assertEquals(Arrays.asList(2L), plan.idsByDiscount.get(10.0));
    }

    @Test
    void noRowsGiveAnEmptyPlan() {
        assertTrue(BhtSummeryController.planTimedServiceDiscount(new ArrayList<>(), 10, null, null).ids.isEmpty());
        assertTrue(BhtSummeryController.planTimedServiceDiscount(null, 10, null, null).ids.isEmpty());
    }
}
