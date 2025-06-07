package com.divudi.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommonFunctionsTest {

    @Test
    public void checkOnlyNumericReturnsTrueForNumericStrings() {
        assertTrue(CommonFunctions.checkOnlyNumeric("12345"));
        assertTrue(CommonFunctions.checkOnlyNumeric("(123) 456-7890"));
        assertTrue(CommonFunctions.checkOnlyNumeric("12+34-56"));
    }

    @Test
    public void checkOnlyNumericReturnsFalseForNonNumericStrings() {
        assertFalse(CommonFunctions.checkOnlyNumeric("123abc"));
        assertFalse(CommonFunctions.checkOnlyNumeric("12#34"));
    }

    @Test
    public void checkOnlyNumericHandlesNull() {
        assertFalse(CommonFunctions.checkOnlyNumeric(null));
    }
}
