package com.divudi.bean.inward;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InwardFormControllerTest {

    @Test
    void clampRadioColumns_returnsChoiceCount_whenWithinRange() {
        assertEquals(3, InwardFormController.clampRadioColumns(3));
    }

    @Test
    void clampRadioColumns_returnsOne_whenChoiceCountIsZero() {
        assertEquals(1, InwardFormController.clampRadioColumns(0));
    }

    @Test
    void clampRadioColumns_returnsOne_whenChoiceCountIsNegative() {
        assertEquals(1, InwardFormController.clampRadioColumns(-5));
    }

    @Test
    void clampRadioColumns_capsAtFour_whenChoiceCountIsLarge() {
        assertEquals(4, InwardFormController.clampRadioColumns(9));
    }
}
