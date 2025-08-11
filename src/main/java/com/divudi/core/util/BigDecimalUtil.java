/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.util;

import java.math.BigDecimal;

/**
 * Utility class for null-safe BigDecimal operations.
 * 
 * This class provides helper methods to handle nullable BigDecimal values
 * consistently throughout the application, addressing the issues identified
 * in GitHub issue #12437.
 * 
 * @author Claude AI Assistant
 */
public class BigDecimalUtil {
    
    /**
     * Returns the BigDecimal value or BigDecimal.ZERO if the value is null.
     * This is the primary method for converting nullable BigDecimal values
     * to non-null values for arithmetic operations.
     * 
     * @param value the BigDecimal value that may be null
     * @return the original value if not null, or BigDecimal.ZERO if null
     */
    public static BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
    
    /**
     * Returns the BigDecimal value or BigDecimal.ONE if the value is null.
     * This is useful for division operations where null should be treated as 1.
     * 
     * @param value the BigDecimal value that may be null
     * @return the original value if not null, or BigDecimal.ONE if null
     */
    public static BigDecimal valueOrOne(BigDecimal value) {
        return value == null ? BigDecimal.ONE : value;
    }
    
    /**
     * Returns null if the BigDecimal value is zero, otherwise returns the value.
     * This is useful when converting explicit zero values back to null for
     * storage optimization.
     * 
     * @param value the BigDecimal value to check
     * @return null if value is zero, otherwise the original value
     */
    public static BigDecimal valueOrNull(BigDecimal value) {
        return (value != null && value.compareTo(BigDecimal.ZERO) == 0) ? null : value;
    }
    
    /**
     * Checks if a BigDecimal value is null or zero.
     * This is useful for validation and conditional logic.
     * 
     * @param value the BigDecimal value to check
     * @return true if value is null or equals zero, false otherwise
     */
    public static boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Null-safe addition of two BigDecimal values.
     * Treats null values as BigDecimal.ZERO.
     * 
     * @param a the first BigDecimal value (may be null)
     * @param b the second BigDecimal value (may be null)
     * @return the sum of the two values, treating null as zero
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return valueOrZero(a).add(valueOrZero(b));
    }
    
    /**
     * Null-safe subtraction of two BigDecimal values.
     * Treats null values as BigDecimal.ZERO.
     * 
     * @param a the minuend BigDecimal value (may be null)
     * @param b the subtrahend BigDecimal value (may be null)  
     * @return the difference of the two values, treating null as zero
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return valueOrZero(a).subtract(valueOrZero(b));
    }
    
    /**
     * Null-safe multiplication of two BigDecimal values.
     * Treats null values as BigDecimal.ZERO.
     * 
     * @param a the first BigDecimal value (may be null)
     * @param b the second BigDecimal value (may be null)
     * @return the product of the two values, treating null as zero
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return valueOrZero(a).multiply(valueOrZero(b));
    }
    
    /**
     * Checks if a BigDecimal value is positive (greater than zero).
     * Returns false for null values.
     * 
     * @param value the BigDecimal value to check
     * @return true if value is not null and greater than zero, false otherwise
     */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Checks if a BigDecimal value is negative (less than zero).
     * Returns false for null values.
     * 
     * @param value the BigDecimal value to check
     * @return true if value is not null and less than zero, false otherwise
     */
    public static boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Checks if a BigDecimal value is zero or negative (less than or equal to zero).
     * Returns true for null values (treated as zero).
     * 
     * @param value the BigDecimal value to check
     * @return true if value is null or less than or equal to zero, false otherwise
     */
    public static boolean isZeroOrNegative(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }
}