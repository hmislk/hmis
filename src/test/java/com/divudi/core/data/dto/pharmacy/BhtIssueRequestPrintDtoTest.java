package com.divudi.core.data.dto.pharmacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BhtIssueRequestPrintDtoTest {

    @Test
    void items_defaultsToEmptyMutableList() {
        BhtIssueRequestPrintDto dto = new BhtIssueRequestPrintDto();
        assertNotNull(dto.getItems());
        assertTrue(dto.getItems().isEmpty());
        dto.getItems().add(new BhtIssueRequestItemPrintDto());
        assertEquals(1, dto.getItems().size());
    }

    @Test
    void stringFields_defaultToEmptyStringNotNull() {
        BhtIssueRequestPrintDto dto = new BhtIssueRequestPrintDto();
        assertEquals("", dto.getFromDepartmentPrintingName());
        assertEquals("", dto.getFromDepartmentName());
        assertEquals("", dto.getPatientName());
        assertEquals("", dto.getRequestedByName());
        assertEquals("", dto.getComments());
    }

    @Test
    void booleanFields_defaultToFalse() {
        BhtIssueRequestPrintDto dto = new BhtIssueRequestPrintDto();
        assertFalse(dto.isCompleted());
        assertFalse(dto.isCancelled());
    }
}
