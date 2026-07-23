package com.divudi.core.util;

import java.security.SecureRandom;

public class ClientPortalOtpGenerator {

    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClientPortalOtpGenerator() {
    }

    public static String generate(int length) {
        StringBuilder otpBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(DIGITS.length());
            otpBuilder.append(DIGITS.charAt(index));
        }
        return otpBuilder.toString();
    }
}
