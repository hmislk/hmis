package com.divudi.bean.inward;

import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.RoomCategory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PackageChangeControllerTest {

    private final PackageChangeController controller = new PackageChangeController();

    @Test
    void mapsAllFieldsOfAPopulatedPackage() {
        AdmissionType admissionType = new AdmissionType();
        admissionType.setName("Surgical");
        RoomCategory roomCategory = new RoomCategory();
        roomCategory.setName("Normal Ward");

        InpatientPackage pkg = new InpatientPackage();
        pkg.setName("Normal Ward Maternity Package");
        pkg.setAdmissionType(admissionType);
        pkg.setRoomCategory(roomCategory);
        pkg.setTotalPrice(50000.0);

        Map<String, Object> state = controller.packageStateMap(pkg);

        assertEquals("Normal Ward Maternity Package", state.get("package"));
        assertEquals("Surgical", state.get("admissionType"));
        assertEquals("Normal Ward", state.get("roomCategory"));
        assertEquals(50000.0, state.get("totalPrice"));
    }

    @Test
    void mapsNullPackageToAllNullValues() {
        Map<String, Object> state = controller.packageStateMap(null);

        assertNull(state.get("package"));
        assertNull(state.get("admissionType"));
        assertNull(state.get("roomCategory"));
        assertNull(state.get("totalPrice"));
    }

    @Test
    void toleratesPackageWithNullAdmissionTypeAndRoomCategory() {
        InpatientPackage pkg = new InpatientPackage();
        pkg.setName("Unassigned Package");
        pkg.setTotalPrice(1000.0);

        Map<String, Object> state = controller.packageStateMap(pkg);

        assertEquals("Unassigned Package", state.get("package"));
        assertNull(state.get("admissionType"));
        assertNull(state.get("roomCategory"));
        assertEquals(1000.0, state.get("totalPrice"));
    }
}
