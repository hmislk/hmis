package com.divudi.ejb;

import com.divudi.core.entity.pharmacy.MeasurementUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrescriptionToItemServiceTest {

    private Double invokeCalculateDosesPerDay(MeasurementUnit unit) throws Exception {
        PrescriptionToItemService service = new PrescriptionToItemService();
        Method method = PrescriptionToItemService.class.getDeclaredMethod("calculateDosesPerDay", MeasurementUnit.class);
        method.setAccessible(true);
        return (Double) method.invoke(service, unit);
    }

    @Test
    public void calculateDosesPerDay_nullName_returnsDefault() throws Exception {
        MeasurementUnit unit = new MeasurementUnit();
        unit.setName(null);
        assertEquals(1.0, invokeCalculateDosesPerDay(unit));
    }

    @Test
    public void calculateDosesPerDay_trimsAndLowercases() throws Exception {
        MeasurementUnit unit = new MeasurementUnit();
        unit.setName("  Twice daily  ");
        assertEquals(2.0, invokeCalculateDosesPerDay(unit));
    }

    @Test
    public void calculateDosesPerDay_normalizesInternalSpaces() throws Exception {
        MeasurementUnit unit = new MeasurementUnit();
        unit.setName("once    daily");
        assertEquals(1.0, invokeCalculateDosesPerDay(unit));
    }
}
