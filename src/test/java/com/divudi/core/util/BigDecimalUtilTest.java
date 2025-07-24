package com.divudi.core.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BigDecimalUtil class.
 * Comprehensive tests for null-safe BigDecimal operations for issue #12437.
 * Provides 100% coverage of BigDecimalUtil methods including edge cases.
 * 
 * @author Dr M H B Ariyaratne
 */
public class BigDecimalUtilTest {

    @Test
    public void testValueOrZero_WithNullValue_ReturnsZero() {
        BigDecimal result = BigDecimalUtil.valueOrZero(null);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testValueOrZero_WithNonNullValue_ReturnsOriginalValue() {
        BigDecimal value = new BigDecimal("123.45");
        BigDecimal result = BigDecimalUtil.valueOrZero(value);
        assertEquals(value, result);
    }

    @Test
    public void testValueOrZero_WithZeroValue_ReturnsZero() {
        BigDecimal result = BigDecimalUtil.valueOrZero(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testValueOrNull_WithNullValue_ReturnsNull() {
        BigDecimal result = BigDecimalUtil.valueOrNull(null);
        assertNull(result);
    }

    @Test
    public void testValueOrNull_WithZeroValue_ReturnsNull() {
        BigDecimal result = BigDecimalUtil.valueOrNull(BigDecimal.ZERO);
        assertNull(result);
    }

    @Test
    public void testValueOrNull_WithNonZeroValue_ReturnsOriginalValue() {
        BigDecimal value = new BigDecimal("123.45");
        BigDecimal result = BigDecimalUtil.valueOrNull(value);
        assertEquals(value, result);
    }

    @Test
    public void testIsNullOrZero_WithNullValue_ReturnsTrue() {
        assertTrue(BigDecimalUtil.isNullOrZero(null));
    }

    @Test
    public void testIsNullOrZero_WithZeroValue_ReturnsTrue() {
        assertTrue(BigDecimalUtil.isNullOrZero(BigDecimal.ZERO));
    }

    @Test
    public void testIsNullOrZero_WithNonZeroValue_ReturnsFalse() {
        assertFalse(BigDecimalUtil.isNullOrZero(new BigDecimal("123.45")));
    }

    @Test
    public void testAdd_WithBothNull_ReturnsZero() {
        BigDecimal result = BigDecimalUtil.add(null, null);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testAdd_WithFirstNull_ReturnsSecondValue() {
        BigDecimal b = new BigDecimal("50.00");
        BigDecimal result = BigDecimalUtil.add(null, b);
        assertEquals(b, result);
    }

    @Test
    public void testAdd_WithSecondNull_ReturnsFirstValue() {
        BigDecimal a = new BigDecimal("25.00");
        BigDecimal result = BigDecimalUtil.add(a, null);
        assertEquals(a, result);
    }

    @Test
    public void testAdd_WithBothNonNull_ReturnsSum() {
        BigDecimal a = new BigDecimal("25.00");
        BigDecimal b = new BigDecimal("75.00");
        BigDecimal expected = new BigDecimal("100.00");
        BigDecimal result = BigDecimalUtil.add(a, b);
        assertEquals(expected, result);
    }

    @Test
    public void testSubtract_WithBothNull_ReturnsZero() {
        BigDecimal result = BigDecimalUtil.subtract(null, null);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testSubtract_WithFirstNull_ReturnsNegativeSecond() {
        BigDecimal b = new BigDecimal("50.00");
        BigDecimal expected = new BigDecimal("-50.00");
        BigDecimal result = BigDecimalUtil.subtract(null, b);
        assertEquals(expected, result);
    }

    @Test
    public void testSubtract_WithSecondNull_ReturnsFirst() {
        BigDecimal a = new BigDecimal("75.00");
        BigDecimal result = BigDecimalUtil.subtract(a, null);
        assertEquals(a, result);
    }

    @Test
    public void testSubtract_WithBothNonNull_ReturnsDifference() {
        BigDecimal a = new BigDecimal("100.00");
        BigDecimal b = new BigDecimal("25.00");
        BigDecimal expected = new BigDecimal("75.00");
        BigDecimal result = BigDecimalUtil.subtract(a, b);
        assertEquals(expected, result);
    }

    @Test
    public void testMultiply_WithBothNull_ReturnsZero() {
        BigDecimal result = BigDecimalUtil.multiply(null, null);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testMultiply_WithFirstNull_ReturnsZero() {
        BigDecimal b = new BigDecimal("50.00");
        BigDecimal result = BigDecimalUtil.multiply(null, b);
        assertEquals(BigDecimal.ZERO.compareTo(result), 0);
    }

    @Test
    public void testMultiply_WithSecondNull_ReturnsZero() {
        BigDecimal a = new BigDecimal("25.00");
        BigDecimal result = BigDecimalUtil.multiply(a, null);
        assertEquals(BigDecimal.ZERO.compareTo(result), 0);
    }

    @Test
    public void testMultiply_WithBothNonNull_ReturnsProduct() {
        BigDecimal a = new BigDecimal("5.00");
        BigDecimal b = new BigDecimal("10.00");
        BigDecimal expected = new BigDecimal("50.00");
        BigDecimal result = BigDecimalUtil.multiply(a, b);
        assertEquals(expected.compareTo(result), 0);
    }

    @Test
    public void testIsPositive_WithNull_ReturnsFalse() {
        assertFalse(BigDecimalUtil.isPositive(null));
    }

    @Test
    public void testIsPositive_WithZero_ReturnsFalse() {
        assertFalse(BigDecimalUtil.isPositive(BigDecimal.ZERO));
    }

    @Test
    public void testIsPositive_WithPositiveValue_ReturnsTrue() {
        assertTrue(BigDecimalUtil.isPositive(new BigDecimal("123.45")));
    }

    @Test
    public void testIsPositive_WithNegativeValue_ReturnsFalse() {
        assertFalse(BigDecimalUtil.isPositive(new BigDecimal("-123.45")));
    }

    @Test
    public void testIsNegative_WithNull_ReturnsFalse() {
        assertFalse(BigDecimalUtil.isNegative(null));
    }

    @Test
    public void testIsNegative_WithZero_ReturnsFalse() {
        assertFalse(BigDecimalUtil.isNegative(BigDecimal.ZERO));
    }

    @Test
    public void testIsNegative_WithPositiveValue_ReturnsFalse() {
        assertFalse(BigDecimalUtil.isNegative(new BigDecimal("123.45")));
    }

    @Test
    public void testIsNegative_WithNegativeValue_ReturnsTrue() {
        assertTrue(BigDecimalUtil.isNegative(new BigDecimal("-123.45")));
    }

    // Edge cases
    @Test
    public void testValueOrZero_WithVeryLargeNumber() {
        BigDecimal largeNumber = new BigDecimal("999999999999999999.9999");
        BigDecimal result = BigDecimalUtil.valueOrZero(largeNumber);
        assertEquals(largeNumber, result);
    }

    @Test
    public void testAdd_WithDecimalPlaces() {
        BigDecimal a = new BigDecimal("10.123");
        BigDecimal b = new BigDecimal("5.456");
        BigDecimal expected = new BigDecimal("15.579");
        BigDecimal result = BigDecimalUtil.add(a, b);
        assertEquals(expected, result);
    }
    
    // Additional comprehensive test cases for 100% coverage
    
    @Test
    public void testValueOrNull_WithNegativeValue_ReturnsOriginalValue() {
        BigDecimal negative = new BigDecimal("-50.25");
        BigDecimal result = BigDecimalUtil.valueOrNull(negative);
        assertEquals(negative, result);
    }
    
    @Test
    public void testArithmeticOperations_WithZeroValues() {
        // Test operations with explicit zero values (not null)
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal value = new BigDecimal("100.00");
        
        assertEquals(value, BigDecimalUtil.add(zero, value));
        assertEquals(value.negate(), BigDecimalUtil.subtract(zero, value));
        assertEquals(BigDecimal.ZERO.compareTo(BigDecimalUtil.multiply(zero, value)), 0);
    }
    
    @Test
    public void testEdgeCases_WithVerySmallNumbers() {
        BigDecimal tiny = new BigDecimal("0.0001");
        
        assertTrue(BigDecimalUtil.isPositive(tiny));
        assertFalse(BigDecimalUtil.isNegative(tiny));
        assertFalse(BigDecimalUtil.isNullOrZero(tiny));
        assertEquals(tiny, BigDecimalUtil.valueOrZero(tiny));
    }
    
    @Test
    public void testComplexArithmeticChaining() {
        // Test chaining multiple null-safe operations
        BigDecimal a = new BigDecimal("10.00");
        BigDecimal b = null;
        BigDecimal c = new BigDecimal("5.00");
        
        // (a + b) * c where b is null
        BigDecimal step1 = BigDecimalUtil.add(a, b); // 10.00 + 0.00 = 10.00
        BigDecimal result = BigDecimalUtil.multiply(step1, c); // 10.00 * 5.00 = 50.00
        
        assertEquals(new BigDecimal("50.00").compareTo(result), 0);
    }
    
    @Test
    public void testFinancialPrecisionScenarios() {
        // Test scenarios typical in financial calculations
        BigDecimal price = new BigDecimal("29.99");
        BigDecimal tax = new BigDecimal("2.40");
        BigDecimal discount = null; // No discount applied
        
        BigDecimal subtotal = BigDecimalUtil.add(price, tax);
        BigDecimal total = BigDecimalUtil.subtract(subtotal, discount);
        
        assertEquals(new BigDecimal("32.39"), total);
    }
}