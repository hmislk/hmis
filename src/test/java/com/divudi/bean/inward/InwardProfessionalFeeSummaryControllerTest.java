package com.divudi.bean.inward;

import com.divudi.core.data.inward.InwardChargeType;
import org.junit.jupiter.api.Test;

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
}
