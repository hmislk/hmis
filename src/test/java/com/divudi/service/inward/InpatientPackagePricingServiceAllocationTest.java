package com.divudi.service.inward;

import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.InpatientPackageItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Issue #25840: a package doctor-fee line covers the row of the staff actually assigned. */
class InpatientPackagePricingServiceAllocationTest {

    private final InpatientPackagePricingService service = new InpatientPackagePricingService();
    private final InwardProfessionalFeeClassificationService classification = new InwardProfessionalFeeClassificationService() {
        @Override
        public boolean isMerged() {
            return false;
        }
    };

    @BeforeEach
    void setUp() {
        service.setProfessionalFeeClassificationService(classification);
    }

    private static InpatientPackageItem component(Speciality speciality) {
        InpatientPackageItem c = new InpatientPackageItem();
        c.setComponentType(InpatientPackageComponentType.PROFESSIONAL_FEE_ROLE);
        c.setFixedPrice(1000.0);
        c.setSpeciality(speciality);
        return c;
    }

    private static BillFee fee(InpatientPackageItem component, Staff staff, InwardChargeType category) {
        BillFee f = new BillFee();
        f.setSourcePackageItem(component);
        f.setStaff(staff);
        f.setProfessionalFeeCategory(category);
        f.setFeeValue(1000.0);
        return f;
    }

    @Test
    void consultantAssigned_coversConsultantRow() {
        InpatientPackageItem c = component(null);
        List<BillFee> fees = Collections.singletonList(fee(c, new Consultant(), InwardChargeType.ProfessionalCharge));
        Map<InwardChargeType, Double> r = service.allocate(Collections.singletonList(c), fees);
        assertEquals(1000.0, r.get(InwardChargeType.ProfessionalCharge));
        assertNull(r.get(InwardChargeType.TechnicianAndParamedicalCharge));
    }

    @Test
    void technicianAssigned_coversTechnicianRow() {
        InpatientPackageItem c = component(null);
        List<BillFee> fees = Collections.singletonList(
                fee(c, new Staff(), InwardChargeType.TechnicianAndParamedicalCharge));
        Map<InwardChargeType, Double> r = service.allocate(Collections.singletonList(c), fees);
        assertEquals(1000.0, r.get(InwardChargeType.TechnicianAndParamedicalCharge));
        assertNull(r.get(InwardChargeType.ProfessionalCharge));
    }

    @Test
    void noneAssigned_coversProfessionalRow() {
        InpatientPackageItem c = component(null);
        List<BillFee> fees = Collections.singletonList(fee(c, null, InwardChargeType.ProfessionalCharge));
        Map<InwardChargeType, Double> r = service.allocate(Collections.singletonList(c), fees);
        assertEquals(1000.0, r.get(InwardChargeType.ProfessionalCharge));
        assertEquals(1, r.size());
        assertEquals(r, service.allocate(Collections.singletonList(c), new ArrayList<>()));
    }

    @Test
    void noneAssigned_withSpecialityDefault_coversThatDefault() {
        Speciality s = new Speciality();
        s.setDefaultProfessionalFeeCategory(InwardChargeType.TechnicianAndParamedicalCharge);
        InpatientPackageItem c = component(s);
        List<BillFee> fees = Collections.singletonList(fee(c, null, InwardChargeType.TechnicianAndParamedicalCharge));
        Map<InwardChargeType, Double> r = service.allocate(Collections.singletonList(c), fees);
        assertEquals(1000.0, r.get(InwardChargeType.TechnicianAndParamedicalCharge));
        assertEquals(1, r.size());
    }

    private InpatientPackagePricingService mergedService() {
        InpatientPackagePricingService merged = new InpatientPackagePricingService();
        merged.setProfessionalFeeClassificationService(new InwardProfessionalFeeClassificationService() {
            @Override
            public boolean isMerged() {
                return true;
            }
        });
        return merged;
    }

    @Test
    void merged_assistantAssigned_coversProfessionalRow() {
        InpatientPackageItem c = component(null);
        List<BillFee> fees = Collections.singletonList(fee(c, new Staff(), InwardChargeType.DoctorAndNurses));
        Map<InwardChargeType, Double> r = mergedService().allocate(Collections.singletonList(c), fees);
        assertEquals(1000.0, r.get(InwardChargeType.ProfessionalCharge));
        assertEquals(1, r.size());
    }

    @Test
    void merged_unassignedWithAssistantSpecialityDefault_coversProfessionalRow() {
        Speciality s = new Speciality();
        s.setDefaultProfessionalFeeCategory(InwardChargeType.DoctorAndNurses);
        InpatientPackageItem c = component(s);
        Map<InwardChargeType, Double> r = mergedService().allocate(Collections.singletonList(c), new ArrayList<>());
        assertEquals(1000.0, r.get(InwardChargeType.ProfessionalCharge));
        assertEquals(1, r.size());
    }

    @Test
    void notMerged_assistantAssigned_coversAssistantRow() {
        InpatientPackageItem c = component(null);
        List<BillFee> fees = Collections.singletonList(fee(c, new Staff(), InwardChargeType.DoctorAndNurses));
        assertEquals(1000.0, service.allocate(Collections.singletonList(c), fees).get(InwardChargeType.DoctorAndNurses));
    }

    @Test
    void unassignedFeeSavedAsTechnician_coversTechnicianRow() {
        InpatientPackageItem c = component(null);
        List<BillFee> fees = Collections.singletonList(
                fee(c, null, InwardChargeType.TechnicianAndParamedicalCharge));
        Map<InwardChargeType, Double> r = service.allocate(Collections.singletonList(c), fees);
        assertEquals(1000.0, r.get(InwardChargeType.TechnicianAndParamedicalCharge));
        assertEquals(1, r.size());
    }

    @Test
    void unassignedFeeWithNoCategory_coversProfessionalRow() {
        InpatientPackageItem c = component(null);
        List<BillFee> fees = Collections.singletonList(fee(c, null, null));
        Map<InwardChargeType, Double> r = service.allocate(Collections.singletonList(c), fees);
        assertEquals(1000.0, r.get(InwardChargeType.ProfessionalCharge));
        assertEquals(1, r.size());
    }
}
