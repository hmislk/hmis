package com.divudi.service.inward;

import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue #23983: technicians are registered as Consultant records so they can be
 * picked on the fee forms, so the staff-subtype rule alone preselected
 * Consultant Fee for them. A speciality's configured default now wins, and the
 * subtype rule stays the fallback so nothing changes until one is configured.
 */
class InwardProfessionalFeeClassificationServiceDefaultCategoryTest {

    private final InwardProfessionalFeeClassificationService service = new InwardProfessionalFeeClassificationService();

    private static Speciality speciality(InwardChargeType defaultCategory) {
        Speciality s = new Speciality();
        s.setDefaultProfessionalFeeCategory(defaultCategory);
        return s;
    }

    private static <T extends Staff> T withSpeciality(T staff, Speciality speciality) {
        staff.setSpeciality(speciality);
        return staff;
    }

    @Test
    void consultantWithTechnicianSpeciality_defaultsToTechnicianFee() {
        Consultant technician = withSpeciality(new Consultant(), speciality(InwardChargeType.TechnicianAndParamedicalCharge));

        assertEquals(InwardChargeType.TechnicianAndParamedicalCharge, service.defaultCategoryFor(technician));
    }

    @Test
    void unconfiguredSpeciality_fallsBackToSubtypeRule() {
        Speciality unconfigured = speciality(null);

        assertEquals(InwardChargeType.ProfessionalCharge,
                service.defaultCategoryFor(withSpeciality(new Consultant(), unconfigured)));
        assertEquals(InwardChargeType.DoctorAndNurses,
                service.defaultCategoryFor(withSpeciality(new Doctor(), unconfigured)));
        assertEquals(InwardChargeType.TechnicianAndParamedicalCharge,
                service.defaultCategoryFor(withSpeciality(new Staff(), unconfigured)));
    }

    @Test
    void selectedSpeciality_winsOverStaffOwnSpeciality() {
        Consultant consultant = withSpeciality(new Consultant(), speciality(InwardChargeType.ProfessionalCharge));

        assertEquals(InwardChargeType.TechnicianAndParamedicalCharge,
                service.defaultCategoryFor(consultant, speciality(InwardChargeType.TechnicianAndParamedicalCharge)));
    }

    @Test
    void unconfiguredSelectedSpeciality_usesStaffOwnSpeciality() {
        Consultant technician = withSpeciality(new Consultant(), speciality(InwardChargeType.TechnicianAndParamedicalCharge));

        assertEquals(InwardChargeType.TechnicianAndParamedicalCharge,
                service.defaultCategoryFor(technician, speciality(null)));
    }

    @Test
    void nonFeeCategoryDefault_isIgnored() {
        Consultant consultant = withSpeciality(new Consultant(), speciality(InwardChargeType.Laboratory));

        assertEquals(InwardChargeType.ProfessionalCharge, service.defaultCategoryFor(consultant));
    }

    @Test
    void noStaff_usesSelectedSpecialityDefault() {
        assertEquals(InwardChargeType.TechnicianAndParamedicalCharge,
                service.defaultCategoryFor(null, speciality(InwardChargeType.TechnicianAndParamedicalCharge)));
    }
}
