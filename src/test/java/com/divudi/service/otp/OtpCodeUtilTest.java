package com.divudi.service.otp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
class OtpCodeUtilTest {

    private static final List<String> SL = Collections.singletonList("+94");
    private static final String KEY = "test-signing-key-0123456789abcdef";

    @Test
    void generatesDigitsOfRequestedLength() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String c = OtpCodeUtil.generateNumericCode(6);
            assertEquals(6, c.length());
            assertTrue(c.matches("\\d{6}"));
            seen.add(c);
        }
        assertTrue(seen.size() > 150, "codes should not repeat often");
        assertThrows(IllegalArgumentException.class, () -> OtpCodeUtil.generateNumericCode(0));
    }

    @Test
    void hashMatchesOnlySameUserPurposeAndCode() {
        String h = OtpCodeUtil.hash(KEY, 10L, "STAFF_LOGIN_2FA", "123456");
        assertEquals(64, h.length());
        assertTrue(OtpCodeUtil.hashesMatch(h, OtpCodeUtil.hash(KEY, 10L, "STAFF_LOGIN_2FA", "123456")));
        assertFalse(OtpCodeUtil.hashesMatch(h, OtpCodeUtil.hash(KEY, 10L, "STAFF_LOGIN_2FA", "123457")));
        // A code issued for another purpose or user never matches
        assertFalse(OtpCodeUtil.hashesMatch(h, OtpCodeUtil.hash(KEY, 10L, "STAFF_MOBILE_VERIFICATION", "123456")));
        assertFalse(OtpCodeUtil.hashesMatch(h, OtpCodeUtil.hash(KEY, 11L, "STAFF_LOGIN_2FA", "123456")));
        // Different key, different hash
        assertNotEquals(h, OtpCodeUtil.hash("another-key", 10L, "STAFF_LOGIN_2FA", "123456"));
        assertFalse(OtpCodeUtil.hashesMatch(h, null));
    }

    @Test
    void hashNeverContainsTheCode() {
        String h = OtpCodeUtil.hash(KEY, 1L, "STAFF_LOGIN_2FA", "987654");
        assertFalse(h.contains("987654"));
    }

    @Test
    void hashRequiresKey() {
        assertThrows(IllegalStateException.class, () -> OtpCodeUtil.hash(null, 1L, "P", "1"));
        assertThrows(IllegalStateException.class, () -> OtpCodeUtil.hash("", 1L, "P", "1"));
    }

    @Test
    void cleanCodeAcceptsDigitsOnly() {
        assertEquals("123456", OtpCodeUtil.cleanCode(" 123 456 "));
        assertNull(OtpCodeUtil.cleanCode("12a456"));
        assertNull(OtpCodeUtil.cleanCode(""));
        assertNull(OtpCodeUtil.cleanCode(null));
    }

    @Test
    void normalisesSriLankanMobileFormats() {
        assertEquals("+94771234567", OtpCodeUtil.normalizeMobile("0771234567", SL));
        assertEquals("+94771234567", OtpCodeUtil.normalizeMobile("771234567", SL));
        assertEquals("+94771234567", OtpCodeUtil.normalizeMobile("94771234567", SL));
        assertEquals("+94771234567", OtpCodeUtil.normalizeMobile("+94 77 123 4567", SL));
        assertEquals("+94771234567", OtpCodeUtil.normalizeMobile("0094-77-123-4567", SL));
    }

    @Test
    void rejectsInvalidOrNotAllowedNumbers() {
        assertNull(OtpCodeUtil.normalizeMobile("+447700900123", SL), "country not allowed");
        assertNull(OtpCodeUtil.normalizeMobile("12345", SL), "too short");
        assertNull(OtpCodeUtil.normalizeMobile("07712abc67", SL));
        assertNull(OtpCodeUtil.normalizeMobile("", SL));
        assertNull(OtpCodeUtil.normalizeMobile(null, SL));
        assertNull(OtpCodeUtil.normalizeMobile("0771234567", Collections.emptyList()));
        assertEquals("+447700900123", OtpCodeUtil.normalizeMobile("+447700900123", Arrays.asList("+94", "+44")));
    }

    @Test
    void parsesCountryCodeConfiguration() {
        assertEquals(Arrays.asList("+94", "+91"), OtpCodeUtil.parseCountryCodes("+94, 91"));
        assertTrue(OtpCodeUtil.parseCountryCodes(null).isEmpty());
    }

    @Test
    void masksMobile() {
        assertEquals("+9477****567", OtpCodeUtil.maskMobile("+94771234567"));
        assertEquals("077****567", OtpCodeUtil.maskMobile("0771234567"));
        assertEquals("****", OtpCodeUtil.maskMobile("12345"));
        assertEquals("", OtpCodeUtil.maskMobile(null));
    }
}
