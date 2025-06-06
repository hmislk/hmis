package com.divudi.core.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommonFunctionsTest {

    @Test
    public void checkOnlyNumericHandlesNull() {
        assertFalse(CommonFunctions.checkOnlyNumeric(null));
    }
}
