package com.divudi.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts the authenticator app secret of each user (WebUser.twoFactorSecret)
 * with AES-256-GCM under a key that is set per server, outside the code and
 * outside the database: the JVM system property {@value #KEY_PROPERTY} or, if
 * that is not set, the environment variable {@value #KEY_ENV}. The value must
 * be a random string of at least {@value #MIN_KEY_LENGTH} characters.
 *
 * Stored format: "gcm1:" + base64(12-byte IV + ciphertext + 16-byte tag).
 * Values without the prefix were written by the older scheme; see
 * TwoFactorAuthController and TwoFactorSecretMigration.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public final class TwoFactorSecretCipher {

    public static final String KEY_PROPERTY = "hmis.twofactor.secretkey";
    public static final String KEY_ENV = "HMIS_TWOFACTOR_SECRET_KEY";
    public static final int MIN_KEY_LENGTH = 32;
    private static final String PREFIX = "gcm1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TwoFactorSecretCipher() {
    }

    /**
     * Whether this server has a usable key. Without one no new secret can be
     * saved.
     */
    public static boolean isKeyConfigured() {
        return keyMaterial() != null;
    }

    /**
     * Whether the stored value was written by this class (and not by the
     * older scheme).
     */
    public static boolean isCurrentFormat(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    /**
     * @return the stored form, or null when the key is not configured or
     * encryption fails
     */
    public static String encrypt(String plain) {
        byte[] key = keyBytes();
        if (key == null || plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return the plain secret, or null when the value is not in this format,
     * the key is missing or wrong, or the value was altered
     */
    public static String decrypt(String stored) {
        byte[] key = keyBytes();
        if (key == null || !isCurrentFormat(stored)) {
            return null;
        }
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (in.length <= IV_BYTES) {
                return null;
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String keyMaterial() {
        String k = System.getProperty(KEY_PROPERTY);
        if (k == null || k.trim().isEmpty()) {
            k = System.getenv(KEY_ENV);
        }
        if (k == null || k.trim().length() < MIN_KEY_LENGTH) {
            return null;
        }
        return k.trim();
    }

    // The configured string is hashed to a 256-bit AES key
    private static byte[] keyBytes() {
        String k = keyMaterial();
        if (k == null) {
            return null;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(k.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }
}
