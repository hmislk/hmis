/*
 * Author : Dr. M H B Ariyaratne
 *
 * Acting Consultant (Health Informatics), Department of Health Services, Southern Province
 * (94) 71 5812399
 * Email : buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.text.BasicTextEncryptor;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class SecurityController implements Serializable {

    private static final SecureRandom random = new SecureRandom();

    private static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String lower = upper.toLowerCase();
    private static final String digits = "0123456789";
    private static final String alphanum = upper + lower + digits;
    private static Map<Character, String> digitMap = new HashMap<>();
    private static Map<String, Character> reverseDigitMap = new HashMap<>();

    private static final long serialVersionUID = 1L;

    static {
        // Initialize the digit mapping
        digitMap.put('0', "Aa");
        digitMap.put('1', "Bb");
        digitMap.put('2', "Cc");
        digitMap.put('3', "Dd");
        digitMap.put('4', "Ee");
        digitMap.put('5', "Ff");
        digitMap.put('6', "Gg");
        digitMap.put('7', "Hh");
        digitMap.put('8', "Ii");
        digitMap.put('9', "Jj");

        // Initialize the reverse mapping
        for (Map.Entry<Character, String> entry : digitMap.entrySet()) {
            reverseDigitMap.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Creates a new instance of HOSecurity
     */
    public SecurityController() {
    }

    private static String insertRandomChars(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(c); // Original character
            // Append a random alphanumeric character
            result.append(alphanum.charAt(random.nextInt(alphanum.length())));
        }
        return result.toString();
    }

    private static String removeRandomChars(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i += 2) {
            result.append(text.charAt(i));
        }
        return result.toString();
    }

    public String encryptAlphanumeric(String text, String key) {
        if (text == null || key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Text and key cannot be null or empty");
        }
        StringBuilder result = new StringBuilder();

        for (int i = 0, j = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                // Encrypt digits using the mapping
                result.append(digitMap.get(c));
            } else if (alphanum.indexOf(c) != -1) {
                // Encrypt letters by shifting them according to the key
                int base = Character.isUpperCase(c) ? 'A' : 'a';
                int keyIndex = alphanum.indexOf(key.charAt(j % key.length()));
                keyIndex = keyIndex < 26 ? keyIndex : keyIndex % 26;
                c = (char) ((c - base + keyIndex) % 26 + base);
                result.append(c);
                j++;
            } else {
                result.append(c);
            }
        }
        return insertRandomChars(result.toString());
    }

    public String decryptAlphanumeric(String text, String key) {
        if (text == null || key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Text and key cannot be null or empty");
        }
        text = removeRandomChars(text);
        StringBuilder result = new StringBuilder();

        for (int i = 0, j = 0; i < text.length(); i++) {
            // Decrypt two characters at a time for digits
            String potentialDigit = text.substring(i, Math.min(i + 2, text.length()));
            if (reverseDigitMap.containsKey(potentialDigit)) {
                // Decrypt digits using the reverse mapping
                result.append(reverseDigitMap.get(potentialDigit));
                i++; // Skip the next character, as it's part of the digit mapping
            } else {
                char c = text.charAt(i);
                if (alphanum.indexOf(c) != -1) {
                    // Decrypt letters by reversing the shifting according to the key
                    int base = Character.isUpperCase(c) ? 'A' : 'a';
                    int keyIndex = alphanum.indexOf(key.charAt(j % key.length()));
                    keyIndex = keyIndex < 26 ? keyIndex : keyIndex % 26;
                    c = (char) ((c - base - keyIndex + 26) % 26 + base);
                    result.append(c);
                    j++;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    public String encrypt(String word) {
        BasicTextEncryptor en = new BasicTextEncryptor();
        en.setPassword("health");
        try {
            return en.encrypt(word);
        } catch (Exception ex) {
            return null;
        }
    }

    private String password;
    private String hashedPassword;
    private boolean matching;

    public void testPassword() {
        matching = matchPassword(password, hashedPassword);
    }

    public String hashAndCheck(String word) {
        try {
            BasicPasswordEncryptor en = new BasicPasswordEncryptor();
            String encryptedPassword = en.encryptPassword(word);
            // This check will always return true for a successfully hashed password
            boolean match = en.checkPassword(word, encryptedPassword);
            if (match) {
                return encryptedPassword;
            } else {
                return null; // This branch will likely never be executed
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean matchPassword(String planePassword, String encryptedPassword) {
        BasicPasswordEncryptor en = new BasicPasswordEncryptor();
        boolean mathingAccess =en.checkPassword(planePassword, encryptedPassword);
        return  mathingAccess;
    }

    public static boolean matchPassword(String planePassword, String encryptedPassword, boolean fake) {
        return true;
    }

    public String generateRandomKey(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be a positive number");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphanum.charAt(random.nextInt(alphanum.length())));
        }
        return sb.toString();
    }

    public String decrypt(String word) {
        BasicTextEncryptor en = new BasicTextEncryptor();
        en.setPassword("health");
        try {
            return en.decrypt(word);
        } catch (Exception ex) {
            return null;
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public boolean isMatching() {
        return matching;
    }

    public void setMatching(boolean matching) {
        this.matching = matching;
    }

}
