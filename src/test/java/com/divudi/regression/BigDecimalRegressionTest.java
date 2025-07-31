package com.divudi.regression;

import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.util.BigDecimalUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for BigDecimal refactoring.
 * Ensures that existing functionality continues to work after the refactoring
 * and that the migration from BigDecimal.ZERO initialization to nullable fields
 * doesn't break any existing business logic.
 * 
 * @author Dr M H B Ariyaratne
 * @since BigDecimal Refactoring Phase 6
 */
@DisplayName("BigDecimal Regression Tests")
public class BigDecimalRegressionTest {
    
    @Nested
    @DisplayName("Legacy Behavior Compatibility Tests")
    class LegacyCompatibilityTest {
        
        @Test
        @DisplayName("Existing Zero Values Should Work Identically to Null")
        public void testExistingZeroValues_ShouldWorkIdenticallyToNull() {
            // Test that existing database records with 0.00 values work the same as new null values
            
            BillItemFinanceDetails legacyItem = new BillItemFinanceDetails();
            legacyItem.setQuantity(BigDecimal.ZERO); // Simulates existing data
            legacyItem.setLineGrossRate(BigDecimal.ZERO);
            legacyItem.setNetTotal(BigDecimal.ZERO);
            
            BillItemFinanceDetails newItem = new BillItemFinanceDetails();
            newItem.setQuantity(null); // New null-based approach
            newItem.setLineGrossRate(null);
            newItem.setNetTotal(null);
            
            // Both should behave identically in calculations
            BigDecimal legacyResult = BigDecimalUtil.multiply(
                legacyItem.getQuantity(), legacyItem.getLineGrossRate()
            );
            
            BigDecimal newResult = BigDecimalUtil.multiply(
                newItem.getQuantity(), newItem.getLineGrossRate()
            );
            
            assertEquals(legacyResult, newResult);
            assertEquals(BigDecimal.ZERO, legacyResult);
            assertEquals(BigDecimal.ZERO, newResult);
        }
        
        @Test
        @DisplayName("Clone Operations Should Work with Both Null and Zero Values")
        public void testCloneOperations_ShouldWorkWithBothNullAndZeroValues() {
            // Test that cloning works correctly with mixed null/zero values
            
            BillFinanceDetails original = new BillFinanceDetails();
            original.setBillDiscount(BigDecimal.ZERO); // Legacy zero value
            original.setLineDiscount(null); // New null value
            original.setGrossTotal(new BigDecimal("100.00")); // Actual value
            original.setNetTotal(BigDecimal.ZERO); // Another legacy zero
            
            BillFinanceDetails cloned = original.clone();
            
            // All values should be preserved exactly
            assertEquals(BigDecimal.ZERO, cloned.getBillDiscount());
            assertNull(cloned.getLineDiscount());
            assertEquals(new BigDecimal("100.00"), cloned.getGrossTotal());
            assertEquals(BigDecimal.ZERO, cloned.getNetTotal());
        }
        
        @Test
        @DisplayName("ToString and HashCode Should Handle Mixed Null/Zero Values")
        public void testToStringAndHashCode_ShouldHandleMixedValues() {
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            item.setQuantity(BigDecimal.ZERO);
            item.setLineGrossRate(null);
            item.setNetTotal(new BigDecimal("50.00"));
            
            // These operations should not throw exceptions
            assertDoesNotThrow(() -> {
                String str = item.toString();
                assertNotNull(str);
                
                int hash = item.hashCode();
                // Hash should be consistent
                assertEquals(hash, item.hashCode());
            });
        }
        
        @Test
        @DisplayName("Equals Method Should Work with Null Values")
        public void testEqualsMethod_ShouldWorkWithNullValues() {
            BillFinanceDetails item1 = new BillFinanceDetails();
            BillFinanceDetails item2 = new BillFinanceDetails();
            
            // Both have null values by default
            item1.setBillDiscount(null);
            item1.setGrossTotal(null);
            item2.setBillDiscount(null);
            item2.setGrossTotal(null);
            
            // Should be equal (based on ID comparison)
            assertDoesNotThrow(() -> {
                boolean isEqual = item1.equals(item2);
                // Actual equality depends on ID, but should not throw exception
            });
        }
    }
    
    @Nested
    @DisplayName("Financial Calculation Regression Tests")
    class FinancialCalculationRegressionTest {
        
        @Test
        @DisplayName("Purchase Total Calculation Should Work with Legacy Data")
        public void testPurchaseTotalCalculation_ShouldWorkWithLegacyData() {
            // Simulate a purchase calculation that would have worked before refactoring
            
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            item.setQuantity(new BigDecimal("10.00"));
            item.setLineGrossRate(new BigDecimal("25.50"));
            item.setLineDiscountRate(BigDecimal.ZERO); // Legacy: no discount
            item.setBillTaxRate(new BigDecimal("8.00"));
            
            // Calculate as before refactoring, but now using null-safe operations
            BigDecimal grossTotal = BigDecimalUtil.multiply(
                item.getQuantity(), item.getLineGrossRate()
            );
            assertEquals(0, new BigDecimal("255.00").compareTo(grossTotal));
            
            BigDecimal discountAmount = BigDecimalUtil.multiply(
                grossTotal, 
                BigDecimalUtil.valueOrZero(item.getLineDiscountRate())
                    .divide(BigDecimal.valueOf(100), BigDecimal.ROUND_HALF_EVEN)
            );
            assertEquals(0, BigDecimal.ZERO.compareTo(discountAmount));
            
            BigDecimal netTotal = BigDecimalUtil.subtract(grossTotal, discountAmount);
            assertEquals(0, new BigDecimal("255.00").compareTo(netTotal));
        }
        
        @Test
        @DisplayName("Multi-Item Bill Calculation Should Work with Mixed Data")
        public void testMultiItemBillCalculation_ShouldWorkWithMixedData() {
            // Test aggregating multiple items with different data patterns
            
            // Item 1: Legacy data pattern (explicit zeros)
            BillItemFinanceDetails item1 = new BillItemFinanceDetails();
            item1.setNetTotal(new BigDecimal("100.00"));
            item1.setBillTax(BigDecimal.ZERO); // Legacy zero
            item1.setLineDiscount(BigDecimal.ZERO);
            
            // Item 2: New data pattern (nulls)
            BillItemFinanceDetails item2 = new BillItemFinanceDetails();
            item2.setNetTotal(new BigDecimal("50.00"));
            item2.setBillTax(null); // New null
            item2.setLineDiscount(null);
            
            // Item 3: Mixed pattern
            BillItemFinanceDetails item3 = new BillItemFinanceDetails();
            item3.setNetTotal(new BigDecimal("25.00"));
            item3.setBillTax(new BigDecimal("2.50"));
            item3.setLineDiscount(BigDecimal.ZERO);
            
            // Aggregate using null-safe operations
            BigDecimal totalNet = BigDecimalUtil.add(
                BigDecimalUtil.add(item1.getNetTotal(), item2.getNetTotal()),
                item3.getNetTotal()
            );
            
            BigDecimal totalTax = BigDecimalUtil.add(
                BigDecimalUtil.add(item1.getBillTax(), item2.getBillTax()),
                item3.getBillTax()
            );
            
            BigDecimal totalDiscount = BigDecimalUtil.add(
                BigDecimalUtil.add(item1.getLineDiscount(), item2.getLineDiscount()),
                item3.getLineDiscount()
            );
            
            // Verify totals
            assertEquals(new BigDecimal("175.00"), totalNet);
            assertEquals(new BigDecimal("2.50"), totalTax);
            assertEquals(BigDecimal.ZERO, totalDiscount);
        }
        
        @Test
        @DisplayName("Percentage Calculations Should Handle Legacy Zero Values")
        public void testPercentageCalculations_ShouldHandleLegacyZeroValues() {
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            item.setGrossTotal(new BigDecimal("100.00"));
            item.setLineDiscountRate(BigDecimal.ZERO); // Legacy: no discount
            item.setBillTaxRate(BigDecimal.ZERO); // Legacy: no tax
            
            // Calculate percentages using null-safe operations
            BigDecimal discountAmount = BigDecimalUtil.multiply(
                item.getGrossTotal(),
                BigDecimalUtil.valueOrZero(item.getLineDiscountRate())
                    .divide(BigDecimal.valueOf(100), BigDecimal.ROUND_HALF_EVEN)
            );
            
            BigDecimal taxAmount = BigDecimalUtil.multiply(
                item.getGrossTotal(),
                BigDecimalUtil.valueOrZero(item.getBillTaxRate())
                    .divide(BigDecimal.valueOf(100), BigDecimal.ROUND_HALF_EVEN)
            );
            
            assertEquals(0, BigDecimal.ZERO.compareTo(discountAmount));
            assertEquals(0, BigDecimal.ZERO.compareTo(taxAmount));
        }
    }
    
    @Nested
    @DisplayName("Data Migration Simulation Tests")
    class DataMigrationSimulationTest {
        
        @Test
        @DisplayName("Pre-Migration Data Pattern Should Continue Working")
        public void testPreMigrationDataPattern_ShouldContinueWorking() {
            // Simulate data that existed before the migration
            
            BillFinanceDetails preMigrationBill = new BillFinanceDetails();
            // These would have been BigDecimal.ZERO before migration
            preMigrationBill.setBillDiscount(BigDecimal.ZERO);
            preMigrationBill.setLineDiscount(BigDecimal.ZERO);
            preMigrationBill.setTotalDiscount(BigDecimal.ZERO);
            preMigrationBill.setGrossTotal(new BigDecimal("1000.00"));
            preMigrationBill.setNetTotal(new BigDecimal("1000.00"));
            
            // Operations should work exactly as before
            BigDecimal totalDiscounts = BigDecimalUtil.add(
                BigDecimalUtil.add(preMigrationBill.getBillDiscount(), preMigrationBill.getLineDiscount()),
                preMigrationBill.getTotalDiscount()
            );
            assertEquals(BigDecimal.ZERO, totalDiscounts);
            
            // Validation should work as expected
            assertFalse(BigDecimalUtil.isPositive(preMigrationBill.getBillDiscount()));
            assertTrue(BigDecimalUtil.isNullOrZero(preMigrationBill.getLineDiscount()));
            assertTrue(BigDecimalUtil.isPositive(preMigrationBill.getGrossTotal()));
        }
        
        @Test
        @DisplayName("Post-Migration Data Pattern Should Work Correctly")
        public void testPostMigrationDataPattern_ShouldWorkCorrectly() {
            // Simulate data created after migration (with nulls)
            
            BillFinanceDetails postMigrationBill = new BillFinanceDetails();
            postMigrationBill.setBillDiscount(null); // No discount applied
            postMigrationBill.setLineDiscount(null); // No line discounts
            postMigrationBill.setTotalDiscount(null); // No total discount
            postMigrationBill.setGrossTotal(new BigDecimal("1000.00"));
            postMigrationBill.setNetTotal(new BigDecimal("1000.00"));
            
            // Operations should produce identical results to pre-migration
            BigDecimal totalDiscounts = BigDecimalUtil.add(
                BigDecimalUtil.add(postMigrationBill.getBillDiscount(), postMigrationBill.getLineDiscount()),
                postMigrationBill.getTotalDiscount()
            );
            assertEquals(BigDecimal.ZERO, totalDiscounts);
            
            // Validation should work identically
            assertFalse(BigDecimalUtil.isPositive(postMigrationBill.getBillDiscount()));
            assertTrue(BigDecimalUtil.isNullOrZero(postMigrationBill.getLineDiscount()));
            assertTrue(BigDecimalUtil.isPositive(postMigrationBill.getGrossTotal()));
        }
        
        @Test
        @DisplayName("Mixed Pre and Post Migration Data Should Coexist")
        public void testMixedPreAndPostMigrationData_ShouldCoexist() {
            // Test scenario where some data is legacy (zeros) and some is new (nulls)
            
            BillItemFinanceDetails legacyItem = new BillItemFinanceDetails();
            legacyItem.setQuantity(new BigDecimal("5.00"));
            legacyItem.setFreeQuantity(BigDecimal.ZERO); // Legacy zero
            legacyItem.setLineGrossRate(new BigDecimal("10.00"));
            
            BillItemFinanceDetails newItem = new BillItemFinanceDetails();
            newItem.setQuantity(new BigDecimal("3.00"));
            newItem.setFreeQuantity(null); // New null
            newItem.setLineGrossRate(new BigDecimal("15.00"));
            
            // Calculate totals - should work seamlessly
            BigDecimal legacyTotal = BigDecimalUtil.add(
                legacyItem.getQuantity(), legacyItem.getFreeQuantity()
            );
            
            BigDecimal newTotal = BigDecimalUtil.add(
                newItem.getQuantity(), newItem.getFreeQuantity()
            );
            
            BigDecimal grandTotal = BigDecimalUtil.add(legacyTotal, newTotal);
            
            assertEquals(new BigDecimal("5.00"), legacyTotal); // 5 + 0
            assertEquals(new BigDecimal("3.00"), newTotal);    // 3 + 0 (null treated as 0)
            assertEquals(new BigDecimal("8.00"), grandTotal);
        }
    }
    
    @Test
    @DisplayName("Critical Business Logic Should Remain Unchanged")
    public void testCriticalBusinessLogic_ShouldRemainUnchanged() {
        // Test that critical business logic continues to work as expected
        
        BillItemFinanceDetails item = new BillItemFinanceDetails();
        
        // unitsPerPack should ALWAYS have a value (never null)
        assertNotNull(item.getUnitsPerPack());
        assertEquals(BigDecimal.ONE, item.getUnitsPerPack());
        
        // Setting unitsPerPack should work
        item.setUnitsPerPack(new BigDecimal("12.00"));
        assertEquals(new BigDecimal("12.00"), item.getUnitsPerPack());
        
        // Calculations using unitsPerPack should work reliably
        BigDecimal packQuantity = new BigDecimal("5.00");
        BigDecimal totalUnits = BigDecimalUtil.multiply(packQuantity, item.getUnitsPerPack());
        assertEquals(0, new BigDecimal("60.00").compareTo(totalUnits));
    }
    
    @Test
    @DisplayName("Error Handling Should Remain Robust")
    public void testErrorHandling_ShouldRemainRobust() {
        // Test that error handling works correctly with nullable fields
        
        BillItemFinanceDetails item = new BillItemFinanceDetails();
        
        // Operations with all null values should not throw exceptions
        assertDoesNotThrow(() -> {
            BigDecimal result = BigDecimalUtil.add(item.getQuantity(), item.getFreeQuantity());
            assertEquals(BigDecimal.ZERO, result);
            
            result = BigDecimalUtil.multiply(item.getLineGrossRate(), item.getQuantity());
            assertEquals(BigDecimal.ZERO, result);
            
            boolean isValid = BigDecimalUtil.isPositive(item.getNetTotal());
            assertFalse(isValid);
        });
        
        // Validation should work correctly
        assertTrue(BigDecimalUtil.isNullOrZero(item.getQuantity()));
        assertFalse(BigDecimalUtil.isPositive(item.getLineGrossRate()));
        assertFalse(BigDecimalUtil.isNegative(item.getNetTotal()));
    }
}