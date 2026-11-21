package com.divudi.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts the authenticator app secret of each user (WebUser.twoFactorSecret)
 * with AES-256-GCM under a key that is set per server, outside the code and
 * outside the database. The key is read, in this order, from: the file named
 * by the environment variable {@value #KEY_FILE_ENV} or the JVM property
 * {@value #KEY_FILE_PROPERTY} (first line; keep the file readable by the
 * server user only), the environment variable {@value #KEY_ENV}, the JVM
 * property {@value #KEY_PROPERTY}. The value must be a random string of at
 * least {@value #MIN_KEY_LENGTH} characters with at least {@value #MIN_DISTINCT_CHARS}
 * different characters (generate with openssl rand -base64 48); a weaker
 * value is logged and treated as no key.
 *
 * Stored formats: "gcm2:" + base64(IV + ciphertext + tag), the user id being
 * authenticated data so a value cannot be moved to another user; "gcm1:"
 * (same without the user id, read and then written as gcm2). Values without
 * a prefix were written by the older scheme; see TwoFactorAuthController
 * and TwoFactorSecretMigration.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public final class TwoFactorSecretCipher {

    public static final String KEY_PROPERTY = "hmis.twofactor.secretkey";
    public static final String KEY_ENV = "HMIS_TWOFACTOR_SECRET_KEY";
    public static final String KEY_FILE_ENV = "HMIS_TWOFACTOR_SECRET_KEY_FILE";
    public static final String KEY_FILE_PROPERTY = "hmis.twofactor.secretkeyfile";
    public static final int MIN_KEY_LENGTH = 32;
    public static final int MIN_DISTINCT_CHARS = 16;
    private static final String PREFIX = "gcm2:";
    private static final String PREFIX_V1 = "gcm1:";
    private static final Logger LOGGER = Logger.getLogger(TwoFactorSecretCipher.class.getName());
    private static volatile String lastWeakKeyReport;
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
     * Whether the stored value is in the newest format (gcm2).
     */
    public static boolean isCurrentFormat(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    /**
     * Whether the stored value was written by this class in any version
     * (gcm1 or gcm2), as opposed to the older scheme.
     */
    public static boolean isAnyGcmFormat(String stored) {
        return stored != null && (stored.startsWith(PREFIX) || stored.startsWith(PREFIX_V1));
    }

    /**
     * @return the stored form, or null when the key is not configured or
     * encryption fails
     */
    public static String encrypt(String plain, Long userId) {
        byte[] key = keyBytes();
        if (key == null || plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(userId));
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
    public static String decrypt(String stored, Long userId) {
        byte[] key = keyBytes();
        if (key == null || !isAnyGcmFormat(stored) || userId == null) {
            return null;
        }
        boolean v2 = isCurrentFormat(stored);
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (in.length <= IV_BYTES) {
                return null;
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            if (v2) {
                cipher.updateAAD(aad(userId));
            }
            byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] aad(Long userId) {
        return ("hmis-2fa-user:" + userId).getBytes(StandardCharsets.UTF_8);
    }

    private static String keyMaterial() {
        String k = null;
        String file = System.getenv(KEY_FILE_ENV);
        if (file == null || file.trim().isEmpty()) {
            file = System.getProperty(KEY_FILE_PROPERTY);
        }
        if (file != null && !file.trim().isEmpty()) {
            try {
                java.util.List<String> lines = Files.readAllLines(Paths.get(file.trim()), StandardCharsets.UTF_8);
                k = lines.isEmpty() ? null : lines.get(0);
            } catch (Exception e) {
                report("The two-factor key file cannot be read; treating the key as missing.");
                return null;
            }
        }
        if (k == null || k.trim().isEmpty()) {
            k = System.getenv(KEY_ENV);
        }
        if (k == null || k.trim().isEmpty()) {
            k = System.getProperty(KEY_PROPERTY);
        }
        if (k == null || k.trim().isEmpty()) {
            return null;
        }
        k = k.trim();
        Set<Character> distinct = new HashSet<>();
        for (char c : k.toCharArray()) {
            distinct.add(c);
        }
        if (k.length() < MIN_KEY_LENGTH || distinct.size() < MIN_DISTINCT_CHARS) {
            report("The two-factor secret key is too weak (needs at least " + MIN_KEY_LENGTH + " characters and "
                    + MIN_DISTINCT_CHARS + " different characters; generate one with openssl rand -base64 48); treating the key as missing.");
            return null;
        }
        return k;
    }

    private static void report(String message) {
        // Logged once per distinct message, not on every check
        if (!message.equals(lastWeakKeyReport)) {
            lastWeakKeyReport = message;
            LOGGER.log(Level.SEVERE, message);
        }
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
