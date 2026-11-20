package com.divudi.bean.inward;

import com.divudi.core.data.inward.InwardChargeType;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InwardProfessionalFeeSummaryControllerTest {

    @Test
    void sumProfessionalGross_returnsProfessionalChargeOnly() {
        Map<InwardChargeType, Double> charges = new EnumMap<>(InwardChargeType.class);
        charges.put(InwardChargeType.ProfessionalCharge, 3000.0);
        charges.put(InwardChargeType.DoctorAndNurses, 500.0);
        charges.put(InwardChargeType.RoomCharges, 1250.0);

        assertEquals(3000.0, InwardProfessionalFeeSummaryController.sumProfessionalGross(charges));
    }

    @Test
    void sumProfessionalGross_returnsZero_whenNoProfessionalChargeEntry() {
        Map<InwardChargeType, Double> charges = new EnumMap<>(InwardChargeType.class);
        charges.put(InwardChargeType.RoomCharges, 1250.0);

        assertEquals(0.0, InwardProfessionalFeeSummaryController.sumProfessionalGross(charges));
    }

    @Test
    void sumProfessionalGross_returnsZero_whenMapIsNull() {
        assertEquals(0.0, InwardProfessionalFeeSummaryController.sumProfessionalGross(null));
    }

    @Test
    void sumOtherGross_excludesProfessionalCharge_includesDoctorAndNurses() {
        Map<InwardChargeType, Double> charges = new EnumMap<>(InwardChargeType.class);
        charges.put(InwardChargeType.ProfessionalCharge, 3000.0);
        charges.put(InwardChargeType.DoctorAndNurses, 500.0);
        charges.put(InwardChargeType.RoomCharges, 1250.0);

        assertEquals(1750.0, InwardProfessionalFeeSummaryController.sumOtherGross(charges));
    }

    @Test
    void sumOtherGross_returnsZero_whenMapIsNull() {
        assertEquals(0.0, InwardProfessionalFeeSummaryController.sumOtherGross(null));
    }

    @Test
    void sumOtherGross_returnsZero_whenMapIsEmpty() {
        assertEquals(0.0, InwardProfessionalFeeSummaryController.sumOtherGross(new EnumMap<>(InwardChargeType.class)));
    }

    @Test
    void toDate_defaultsToEndOfDay() {
        InwardProfessionalFeeSummaryController controller = new InwardProfessionalFeeSummaryController();
        Date toDate = controller.getToDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(toDate);

        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
    }

    @Test
    void makeNull_resetsToDateToEndOfDay() {
        InwardProfessionalFeeSummaryController controller = new InwardProfessionalFeeSummaryController();
        controller.setToDate(new Date(0));
        controller.makeNull();

        Date toDate = controller.getToDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(toDate);

        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
    }

    @Test
    void sumOtherGross_leavesOutComputedVat() {
        Map<InwardChargeType, Double> charges = new EnumMap<>(InwardChargeType.class);
        charges.put(InwardChargeType.RoomCharges, 1000.0);
        charges.put(InwardChargeType.ComputedVAT, 180.0);

        assertEquals(1000.0, InwardProfessionalFeeSummaryController.sumOtherGross(charges));
    }
}
