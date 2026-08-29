package com.divudi.service.pharmacy;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class BhtIssueRequestNativeSqlServiceTest {

    private final BhtIssueRequestNativeSqlService service = new BhtIssueRequestNativeSqlService();

    @Test
    void str_returnsEmptyStringForNull() {
        assertEquals("", service.str(null));
    }

    @Test
    void str_trimsValue() {
        assertEquals("Ward 3", service.str("  Ward 3  "));
    }

    @Test
    void toBool_handlesBooleanNumberAndNull() {
        assertFalse(service.toBool(null));
        assertTrue(service.toBool(Boolean.TRUE));
        assertFalse(service.toBool(Boolean.FALSE));
        assertTrue(service.toBool(1));
        assertFalse(service.toBool(0));
    }

    @Test
    void toDate_convertsTimestamp() {
        long millis = System.currentTimeMillis();
        Date result = service.toDate(new Timestamp(millis));
        assertEquals(millis, result.getTime());
    }

    @Test
    void toDate_returnsNullForNull() {
        assertNull(service.toDate(null));
    }

    @Test
    void titleLabel_returnsEmptyForNullOrBlank() {
        assertEquals("", service.titleLabel(null));
        assertEquals("", service.titleLabel("   "));
    }

    @Test
    void titleLabel_returnsEmptyForUnknownTitle() {
        assertEquals("", service.titleLabel("NotARealTitle"));
    }

    @Test
    void titleLabel_returnsLabelForKnownTitle() {
        assertEquals("Mr. ", service.titleLabel("Mr"));
    }
}
