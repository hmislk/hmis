package com.divudi.bean.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class PatientReportControllerParseTest {

    @Test
    public void plainAndCommaNumbersParse() {
        assertEquals(12.5, PatientReportController.parseTypedNumber(" 12.5 "));
        assertEquals(1234.5, PatientReportController.parseTypedNumber("1,234.5"));
        assertEquals(0.0, PatientReportController.parseTypedNumber("0"));
    }

    @Test
    public void nonNumericTextIsNull() {
        assertNull(PatientReportController.parseTypedNumber(null));
        assertNull(PatientReportController.parseTypedNumber(""));
        assertNull(PatientReportController.parseTypedNumber("<0.001"));
        assertNull(PatientReportController.parseTypedNumber(">100"));
        assertNull(PatientReportController.parseTypedNumber("NaN"));
        assertNull(PatientReportController.parseTypedNumber("Infinity"));
    }
}
