package com.divudi.bean.inward;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InwardChargeTypeDetailControllerTest {

    @Test
    void toDate_defaultsToEndOfDay() {
        InwardChargeTypeDetailController controller = new InwardChargeTypeDetailController();
        Date toDate = controller.getToDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(toDate);

        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
    }

    @Test
    void makeNull_resetsToDateToEndOfDay() {
        InwardChargeTypeDetailController controller = new InwardChargeTypeDetailController();
        controller.setToDate(new Date(0));
        controller.makeNull();

        Date toDate = controller.getToDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(toDate);

        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
    }
}
