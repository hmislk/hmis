package com.divudi.service.inward;

import com.divudi.core.data.inward.InwardChargeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Issues #25031 / #25030: when professional and assisting fees are merged,
 * a charge filed under Assistant Fee must be shown and totalled under
 * Professional Charge, since the Assistant Fee row does not exist.
 */
class InwardProfessionalFeeClassificationServiceRowTypeTest {

    private static InwardProfessionalFeeClassificationService service(boolean merged) {
        return new InwardProfessionalFeeClassificationService() {
            @Override
            public boolean isMerged() {
                return merged;
            }
        };
    }

    @Test
    void merged_assistantFeeMapsToProfessionalCharge() {
        assertEquals(InwardChargeType.ProfessionalCharge,
                service(true).rowTypeFor(InwardChargeType.DoctorAndNurses));
    }

    @Test
    void notMerged_assistantFeeKeepsItsOwnRow() {
        assertEquals(InwardChargeType.DoctorAndNurses,
                service(false).rowTypeFor(InwardChargeType.DoctorAndNurses));
    }

    @Test
    void otherTypesMapToThemselvesEitherWay() {
        for (boolean merged : new boolean[]{true, false}) {
            InwardProfessionalFeeClassificationService s = service(merged);
            assertEquals(InwardChargeType.ProfessionalCharge, s.rowTypeFor(InwardChargeType.ProfessionalCharge));
            assertEquals(InwardChargeType.TechnicianAndParamedicalCharge,
                    s.rowTypeFor(InwardChargeType.TechnicianAndParamedicalCharge));
            assertEquals(InwardChargeType.Laboratory, s.rowTypeFor(InwardChargeType.Laboratory));
            assertNull(s.rowTypeFor(null));
        }
    }
}
