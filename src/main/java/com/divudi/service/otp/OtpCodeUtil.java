package com.divudi.service.otp;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Stateless helpers for one-time codes: generation, hashing, comparison,
 * mobile number normalisation and masking. Kept free of EJB/JSF so it can be
 * unit tested directly.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public final class OtpCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpCodeUtil() {
    }

    /**
     * Random numeric code from SecureRandom (never java.util.Random).
     */
    public static String generateNumericCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Code length must be positive");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Hex HMAC-SHA256 of "userId:purpose:code". Binding the user and purpose
     * into the hash means a code for one user or purpose can never match
     * another.
     */
    public static String hash(String key, Long userId, String purpose, String code) {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("OTP signing key is not available");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((userId + ":" + purpose + ":" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    /**
     * Constant-time comparison of two hashes, so the time taken does not
     * reveal how many leading characters matched.
     */
    public static boolean hashesMatch(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Digits only, after removing spaces/dashes; null when the input has
     * anything else.
     */
    public static String cleanCode(String code) {
        if (code == null) {
            return null;
        }
        String c = code.replaceAll("\\s", "");
        return c.matches("\\d+") ? c : null;
    }

    /**
     * Parses "+94, +91" style configuration into a list of codes like
     * "+94".
     */
    public static List<String> parseCountryCodes(String configured) {
        List<String> codes = new ArrayList<>();
        if (configured == null) {
            return codes;
        }
        for (String part : configured.split("[,;\\s]+")) {
            String digits = part.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                codes.add("+" + digits);
            }
        }
        return codes;
    }

    /**
     * Converts a mobile number to E.164 (e.g. 0771234567, 771234567,
     * 94771234567 and +94 77 123 4567 all become +94771234567). Numbers
     * without a country code are taken to be in the first allowed country.
     * Returns null if the number is not valid or its country is not allowed.
     */
    public static String normalizeMobile(String raw, List<String> allowedCountryCodes) {
        if (raw == null || allowedCountryCodes == null || allowedCountryCodes.isEmpty()) {
            return null;
        }
        String s = raw.trim().replaceAll("[\\s\\-().]", "");
        if (s.isEmpty()) {
            return null;
        }
        boolean international = false;
        if (s.startsWith("+")) {
            international = true;
            s = s.substring(1);
        } else if (s.startsWith("00")) {
            international = true;
            s = s.substring(2);
        }
        if (!s.matches("\\d+")) {
            return null;
        }
        String e164 = null;
        if (international) {
            e164 = "+" + s;
        } else {
            for (String cc : allowedCountryCodes) {
                String ccDigits = cc.substring(1);
                if (s.startsWith(ccDigits) && s.length() > ccDigits.length() + 7) {
                    e164 = "+" + s;
                    break;
                }
            }
            if (e164 == null) {
                String national = s.startsWith("0") ? s.substring(1) : s;
                e164 = allowedCountryCodes.get(0) + national;
            }
        }
        // E.164: at most 15 digits; a mobile number has at least 8 digits after the country code
        int digits = e164.length() - 1;
        if (digits < 10 || digits > 15) {
            return null;
        }
        for (String cc : allowedCountryCodes) {
            if (e164.startsWith(cc)) {
                return e164;
            }
        }
        return null;
    }

    /**
     * +94771234567 becomes +9477****567. Used in the UI and audit logs.
     */
    public static String maskMobile(String mobile) {
        if (mobile == null) {
            return "";
        }
        String m = mobile.trim();
        if (m.length() <= 7) {
            return "****";
        }
        int keepStart = m.startsWith("+") ? 5 : 3;
        int keepEnd = 3;
        StringBuilder sb = new StringBuilder();
        sb.append(m, 0, keepStart);
        for (int i = keepStart; i < m.length() - keepEnd; i++) {
            sb.append('*');
        }
        sb.append(m.substring(m.length() - keepEnd));
        return sb.toString();
    }
}
