package com.divudi.core.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Time-based one-time passwords (RFC 6238, HMAC-SHA1, 6 digits, 30 second
 * step) as used by Google Authenticator, Microsoft Authenticator, Authy etc.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public class TotpUtil {

    public static final int DIGITS = 6;
    public static final int PERIOD_SECONDS = 30;
    /**
     * Number of steps either side of "now" accepted, to tolerate clock drift
     * between the server and the phone.
     */
    public static final int ALLOWED_DRIFT_STEPS = 1;

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpUtil() {
    }

    /**
     * @return a new random 160-bit secret, Base32 encoded (32 characters).
     */
    public static String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public static long currentStep() {
        return System.currentTimeMillis() / 1000L / PERIOD_SECONDS;
    }

    /**
     * Verifies a code against the secret.
     *
     * @param base32Secret the shared secret
     * @param code the code typed by the user
     * @param lastUsedStep the last step already accepted for this user, or
     * null; codes at or before it are rejected to block replay
     * @return the matched step, or -1 when the code is not valid
     */
    public static long verify(String base32Secret, String code, Long lastUsedStep) {
        if (base32Secret == null || code == null) {
            return -1;
        }
        String cleaned = code.replaceAll("\\s", "");
        if (!cleaned.matches("\\d{" + DIGITS + "}")) {
            return -1;
        }
        byte[] key = base32Decode(base32Secret);
        long now = currentStep();
        for (long step = now - ALLOWED_DRIFT_STEPS; step <= now + ALLOWED_DRIFT_STEPS; step++) {
            if (lastUsedStep != null && step <= lastUsedStep) {
                continue;
            }
            String expected = generateCode(key, step);
            if (MessageDigest.isEqual(expected.getBytes(), cleaned.getBytes())) {
                return step;
            }
        }
        return -1;
    }

    static String generateCode(byte[] key, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 not available", e);
        }
    }

    /**
     * Builds the otpauth:// URI that authenticator apps read from the QR code.
     */
    public static String buildOtpAuthUri(String issuer, String accountName, String base32Secret) {
        String safeIssuer = (issuer == null || issuer.trim().isEmpty()) ? "HMIS" : issuer.trim();
        String label = urlEncode(safeIssuer) + ":" + urlEncode(accountName);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + urlEncode(safeIssuer)
                + "&algorithm=SHA1&digits=" + DIGITS
                + "&period=" + PERIOD_SECONDS;
    }

    /**
     * Renders text as an inline SVG QR code.
     */
    public static String toQrSvg(String text, int sizePx) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints);
            int w = matrix.getWidth();
            int h = matrix.getHeight();
            StringBuilder path = new StringBuilder();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (matrix.get(x, y)) {
                        path.append('M').append(x).append(',').append(y).append("h1v1h-1z");
                    }
                }
            }
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + sizePx + "\" height=\"" + sizePx
                    + "\" viewBox=\"0 0 " + w + " " + h + "\" shape-rendering=\"crispEdges\">"
                    + "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"
                    + "<path fill=\"#000000\" d=\"" + path + "\"/></svg>";
        } catch (WriterException e) {
            return "";
        }
    }

    /**
     * Groups the secret in blocks of four for manual entry.
     */
    public static String formatSecretForDisplay(String base32Secret) {
        if (base32Secret == null) {
            return "";
        }
        return base32Secret.replaceAll("(.{4})(?!$)", "$1 ");
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    static byte[] base32Decode(String s) {
        String cleaned = s.replaceAll("[\\s=-]", "").toUpperCase();
        ByteBuffer out = ByteBuffer.allocate(cleaned.length() * 5 / 8);
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : cleaned.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.put((byte) ((buffer >> (bitsLeft - 8)) & 0xFF));
                bitsLeft -= 8;
            }
        }
        byte[] result = new byte[out.position()];
        out.flip();
        out.get(result);
        return result;
    }
}
