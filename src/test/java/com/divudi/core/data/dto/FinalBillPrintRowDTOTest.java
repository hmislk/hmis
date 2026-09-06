package com.divudi.core.data.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinalBillPrintRowDTOTest {

    @Test
    void constructor_setsAllThreeFields() {
        FinalBillPrintRowDTO row = new FinalBillPrintRowDTO("Room Charges", 4250.0, 30);

        assertEquals("Room Charges", row.getLabel());
        assertEquals(4250.0, row.getAmount());
        assertEquals(30, row.getOrder());
    }
}
