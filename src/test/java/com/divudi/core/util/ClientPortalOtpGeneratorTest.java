package com.divudi.core.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClientPortalOtpGeneratorTest {

    @Test
    public void testGeneratesRequestedLength() {
        String otp = ClientPortalOtpGenerator.generate(6);
        assertEquals(6, otp.length());
    }

    @Test
    public void testGeneratesOnlyDigits() {
        String otp = ClientPortalOtpGenerator.generate(8);
        assertTrue(otp.matches("[0-9]+"));
    }

    @Test
    public void testDifferentLengthProducesDifferentSize() {
        String otp = ClientPortalOtpGenerator.generate(4);
        assertEquals(4, otp.length());
    }

    @Test
    public void testZeroLengthProducesEmptyString() {
        String otp = ClientPortalOtpGenerator.generate(0);
        assertEquals("", otp);
    }
}
