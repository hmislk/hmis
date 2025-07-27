package com.divudi.core.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests null-handling behavior in BigDecimal entity fields. Validates that
 * entities properly handle nullable BigDecimal fields after the BigDecimal
 * refactoring initiative.
 *
 * @author Dr M H B Ariyaratne
 * @since BigDecimal Refactoring Phase 6
 */
@DisplayName("Entity Null Handling Tests")
public class EntityNullHandlingTest {

    @Nested
    @DisplayName("BillFinanceDetails Entity Tests")
    class BillFinanceDetailsTest {

        @Test
        @DisplayName("Default Constructor Should Create Entity with Null BigDecimal Fields")
        public void testDefaultConstructor_ShouldCreateEntityWithNullFields() {
            BillFinanceDetails entity = new BillFinanceDetails();

            // All BigDecimal fields should be null by default
            assertNull(entity.getBillDiscount());
            assertNull(entity.getLineDiscount());
            assertNull(entity.getTotalDiscount());
            assertNull(entity.getBillExpense());
            assertNull(entity.getLineExpense());
            assertNull(entity.getTotalExpense());
            assertNull(entity.getBillCostValue());
            assertNull(entity.getLineCostValue());
            assertNull(entity.getTotalCostValue());
            assertNull(entity.getBillTaxValue());
            assertNull(entity.getItemTaxValue());
            assertNull(entity.getTotalTaxValue());
            assertNull(entity.getGrossTotal());
            assertNull(entity.getNetTotal());
            assertNull(entity.getTotalQuantity());
            assertNull(entity.getTotalFreeQuantity());
        }

        @Test
        @DisplayName("Setter/Getter Methods Should Handle Null Values")
        public void testSettersGetters_ShouldHandleNullValues() {
            BillFinanceDetails entity = new BillFinanceDetails();

            // Test setting null values
            assertDoesNotThrow(() -> {
                entity.setBillDiscount(null);
                entity.setGrossTotal(null);
                entity.setNetTotal(null);
                entity.setTotalQuantity(null);
            });

            // Test getting null values
            assertNull(entity.getBillDiscount());
            assertNull(entity.getGrossTotal());
            assertNull(entity.getNetTotal());
            assertNull(entity.getTotalQuantity());
        }

        @Test
        @DisplayName("Clone Method Should Preserve Null Values")
        public void testClone_ShouldPreserveNullValues() {
            BillFinanceDetails original = new BillFinanceDetails();

            // Set mix of null and non-null values
            original.setBillDiscount(new BigDecimal("10.00"));
            original.setLineDiscount(null);
            original.setGrossTotal(new BigDecimal("100.00"));
            original.setNetTotal(null);

            BillFinanceDetails cloned = original.clone();

            // Verify null values are preserved
            assertEquals(new BigDecimal("10.00"), cloned.getBillDiscount());
            assertNull(cloned.getLineDiscount());
            assertEquals(new BigDecimal("100.00"), cloned.getGrossTotal());
            assertNull(cloned.getNetTotal());
        }

        @Test
        @DisplayName("Entity Should Handle All Null BigDecimal Fields Without Exception")
        public void testAllNullFields_ShouldNotThrowException() {
            BillFinanceDetails entity = new BillFinanceDetails();

            assertDoesNotThrow(() -> {
                // Test that toString() works with null fields
                String str = entity.toString();
                assertNotNull(str);

                // Test that hashCode() works with null fields  
                int hash = entity.hashCode();

                // Test that equals() works with null fields
                BillFinanceDetails other = new BillFinanceDetails();
                entity.equals(other);
            });
        }
    }

    @Nested
    @DisplayName("BillItemFinanceDetails Entity Tests")
    class BillItemFinanceDetailsTest {

        @Test
        @DisplayName("Default Constructor Should Create Entity with Null Fields and unitsPerPack Default")
        public void testDefaultConstructor_ShouldCreateEntityWithCorrectDefaults() {
            BillItemFinanceDetails entity = new BillItemFinanceDetails();

            // Most BigDecimal fields should be null by default
            assertNull(entity.getQuantity());
            assertNull(entity.getFreeQuantity());
            assertNull(entity.getLineGrossRate());
            assertNull(entity.getBillGrossRate());
            assertNull(entity.getGrossRate());
            assertNull(entity.getNetRate());
            assertNull(entity.getGrossTotal());
            assertNull(entity.getNetTotal());

            // Exception: unitsPerPack should have BigDecimal.ONE default
            assertEquals(BigDecimal.ONE, entity.getUnitsPerPack());
        }

        @Test
        @DisplayName("unitsPerPack Should Maintain Its Default Value")
        public void testUnitsPerPack_ShouldMaintainDefaultValue() {
            BillItemFinanceDetails entity = new BillItemFinanceDetails();

            // unitsPerPack should never be null
            assertNotNull(entity.getUnitsPerPack());
            assertEquals(BigDecimal.ONE, entity.getUnitsPerPack());

            // Setting it to a different value should work
            entity.setUnitsPerPack(new BigDecimal("10.00"));
            assertEquals(new BigDecimal("10.00"), entity.getUnitsPerPack());

            // But it should not accept null (this depends on validation logic)
            // Note: The entity itself doesn't prevent null, but business logic should
        }

        @Test
        @DisplayName("Quantity Fields Should Accept Null Values")
        public void testQuantityFields_ShouldAcceptNullValues() {
            BillItemFinanceDetails entity = new BillItemFinanceDetails();

            // Test all quantity-related fields can be null
            assertDoesNotThrow(() -> {
                entity.setQuantity(null);
                entity.setFreeQuantity(null);
                entity.setTotalQuantity(null);
                entity.setQuantityByUnits(null);
                entity.setFreeQuantityByUnits(null);
                entity.setTotalQuantityByUnits(null);
            });

            // Verify they are actually null
            assertNull(entity.getQuantity());
            assertNull(entity.getFreeQuantity());
            assertNull(entity.getTotalQuantity());
            assertNull(entity.getQuantityByUnits());
            assertNull(entity.getFreeQuantityByUnits());
            assertNull(entity.getTotalQuantityByUnits());
        }

        @Test
        @DisplayName("Rate Fields Should Accept Null Values")
        public void testRateFields_ShouldAcceptNullValues() {
            BillItemFinanceDetails entity = new BillItemFinanceDetails();

            // Test all rate-related fields can be null
            assertDoesNotThrow(() -> {
                entity.setLineGrossRate(null);
                entity.setBillGrossRate(null);
                entity.setGrossRate(null);
                entity.setLineNetRate(null);
                entity.setBillNetRate(null);
                entity.setNetRate(null);
                entity.setRetailSaleRate(null);
                entity.setWholesaleRate(null);
            });

            // Verify they are actually null
            assertNull(entity.getLineGrossRate());
            assertNull(entity.getBillGrossRate());
            assertNull(entity.getGrossRate());
            assertNull(entity.getLineNetRate());
            assertNull(entity.getBillNetRate());
            assertNull(entity.getNetRate());
            assertNull(entity.getRetailSaleRate());
            assertNull(entity.getWholesaleRate());
        }

        @Test
        @DisplayName("Clone Method Should Handle Mixed Null and Non-Null Values")
        public void testClone_ShouldHandleMixedValues() {
            BillItemFinanceDetails original = new BillItemFinanceDetails();

            // Set mixed values including nulls
            original.setQuantity(new BigDecimal("5.00"));
            original.setFreeQuantity(null);
            original.setLineGrossRate(new BigDecimal("20.00"));
            original.setBillGrossRate(null);
            original.setGrossTotal(new BigDecimal("100.00"));
            original.setNetTotal(null);
            // unitsPerPack should maintain its default
            assertEquals(BigDecimal.ONE, original.getUnitsPerPack());

            BillItemFinanceDetails cloned = original.clone();

            // Verify mixed values are preserved correctly
            assertEquals(new BigDecimal("5.00"), cloned.getQuantity());
            assertNull(cloned.getFreeQuantity());
            assertEquals(new BigDecimal("20.00"), cloned.getLineGrossRate());
            assertNull(cloned.getBillGrossRate());
            assertEquals(new BigDecimal("100.00"), cloned.getGrossTotal());
            assertNull(cloned.getNetTotal());
            assertEquals(BigDecimal.ONE, cloned.getUnitsPerPack());
        }

        @Test
        @DisplayName("Entity Should Handle Complex Null Scenarios")
        public void testComplexNullScenarios_ShouldNotThrowException() {
            BillItemFinanceDetails entity = new BillItemFinanceDetails();

            entity.setQuantity(null);
            entity.setLineGrossRate(null);
            entity.setGrossTotal(new BigDecimal("0.00"));
            entity.setNetTotal(null);
            entity.setLineDiscount(null);
            entity.setBillTax(BigDecimal.ZERO);

            // Wrap only the part that might throw due to null handling
            assertDoesNotThrow(() -> {
                entity.toString();
                entity.hashCode();
                entity.equals(new BillItemFinanceDetails());
                entity.clone();
            });

            // Then assert values outside
            BillItemFinanceDetails cloned = entity.clone();
            assertNull(cloned.getQuantity());
            assertNull(cloned.getLineGrossRate());
            assertEquals(BigDecimal.ZERO.compareTo(cloned.getGrossTotal()), 0,
                    "Expected: 0, but was: " + cloned.getGrossTotal());
            assertNull(cloned.getNetTotal());
        }

        @Test
        @DisplayName("Business Critical Fields Should Have Appropriate Defaults")
        public void testBusinessCriticalFields_ShouldHaveDefaults() {
            BillItemFinanceDetails entity = new BillItemFinanceDetails();

            // unitsPerPack is business-critical and should never be null
            assertNotNull(entity.getUnitsPerPack());
            assertEquals(BigDecimal.ONE, entity.getUnitsPerPack());

            // Test that constructor with BillItem parameter also sets defaults
            BillItem billItem = new BillItem(); // Assuming this exists
            BillItemFinanceDetails entityWithBillItem = new BillItemFinanceDetails(billItem);
            assertEquals(BigDecimal.ONE, entityWithBillItem.getUnitsPerPack());
        }
    }

    @Test
    @DisplayName("Cross-Entity Null Value Propagation")
    public void testCrossEntityNullPropagation_ShouldWorkCorrectly() {
        // Test that null values can be properly passed between entities

        BillItemFinanceDetails itemDetails = new BillItemFinanceDetails();
        itemDetails.setGrossTotal(null);
        itemDetails.setNetTotal(new BigDecimal("50.00"));
        itemDetails.setLineDiscount(null);

        BillFinanceDetails billDetails = new BillFinanceDetails();

        // Simulate aggregating item totals to bill totals
        // In real application, this would be done by service layer with BigDecimalUtil
        if (itemDetails.getGrossTotal() != null) {
            billDetails.setGrossTotal(itemDetails.getGrossTotal());
        } else {
            billDetails.setGrossTotal(null); // or BigDecimal.ZERO depending on business logic
        }

        if (itemDetails.getNetTotal() != null) {
            billDetails.setNetTotal(itemDetails.getNetTotal());
        } else {
            billDetails.setNetTotal(null);
        }

        // Verify null propagation
        assertNull(billDetails.getGrossTotal());
        assertEquals(new BigDecimal("50.00"), billDetails.getNetTotal());
    }
}
