package com.divudi.core.data.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InwardProfessionalFeeSummaryRowDtoTest {

    @Test
    void getNetTotal_sumsProfessionalAndOtherFeeTotals() {
        InwardProfessionalFeeSummaryRowDto row = new InwardProfessionalFeeSummaryRowDto();
        row.setProfessionalFeeTotal(3000.0);
        row.setOtherFeeTotal(9500.0);

        assertEquals(12500.0, row.getNetTotal());
    }

    @Test
    void getNetTotal_isZero_whenBothTotalsUnset() {
        InwardProfessionalFeeSummaryRowDto row = new InwardProfessionalFeeSummaryRowDto();

        assertEquals(0.0, row.getNetTotal());
    }

    @Test
    void getNetTotal_handlesNegativeOtherFeeTotal_fromCancellationCredit() {
        InwardProfessionalFeeSummaryRowDto row = new InwardProfessionalFeeSummaryRowDto();
        row.setProfessionalFeeTotal(5000.0);
        row.setOtherFeeTotal(-1200.0);

        assertEquals(3800.0, row.getNetTotal());
    }

    @Test
    void getFinalBillNo_returnsEmptyString_whenNull() {
        InwardProfessionalFeeSummaryRowDto row = new InwardProfessionalFeeSummaryRowDto();

        assertEquals("", row.getFinalBillNo());
    }
}
