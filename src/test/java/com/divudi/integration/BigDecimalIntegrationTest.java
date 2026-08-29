package com.divudi.integration;

import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.util.BigDecimalUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for BigDecimal refactoring initiative.
 * Tests end-to-end financial calculation workflows with nullable BigDecimal fields.
 * 
 * These tests validate that the BigDecimal refactoring works correctly across
 * entity creation, financial calculations, and business logic operations.
 * 
 * @author Dr M H B Ariyaratne
 * @since BigDecimal Refactoring Phase 6
 */
@DisplayName("BigDecimal Refactoring Integration Tests")
public class BigDecimalIntegrationTest {
    
    private BillFinanceDetails billFinanceDetails;
    private BillItemFinanceDetails billItemFinanceDetails;
    
    @BeforeEach
    public void setUp() {
        billFinanceDetails = new BillFinanceDetails();
        billItemFinanceDetails = new BillItemFinanceDetails();
    }
    
    @Test
    @DisplayName("Entity Creation with Null BigDecimal Fields")
    public void testEntityCreation_WithNullFields_ShouldNotThrowException() {
        // Test that entities can be created with all null BigDecimal fields
        assertDoesNotThrow(() -> {
            BillFinanceDetails bfd = new BillFinanceDetails();
            
            // All BigDecimal fields should be null by default (no BigDecimal.ZERO initialization)
            assertNull(bfd.getBillDiscount());
            assertNull(bfd.getLineDiscount());
            assertNull(bfd.getTotalDiscount());
            assertNull(bfd.getGrossTotal());
            assertNull(bfd.getNetTotal());
        });
        
        assertDoesNotThrow(() -> {
            BillItemFinanceDetails bifd = new BillItemFinanceDetails();
            
            // Most BigDecimal fields should be null by default
            assertNull(bifd.getQuantity());
            assertNull(bifd.getLineGrossRate());
            assertNull(bifd.getNetTotal());
            
            // Exception: unitsPerPack should have default value
            assertEquals(BigDecimal.ONE, bifd.getUnitsPerPack());
        });
    }
    
    @Test
    @DisplayName("Financial Calculation Workflow with Mixed Null/Non-Null Values")
    public void testFinancialCalculation_WithMixedNullValues_ShouldProduceCorrectResults() {
        // Simulate a financial calculation scenario with some null values
        
        // Set up bill item with partial data (some fields null)
        billItemFinanceDetails.setQuantity(new BigDecimal("10.00"));
        billItemFinanceDetails.setLineGrossRate(new BigDecimal("25.50"));
        billItemFinanceDetails.setLineDiscountRate(null); // No discount
        billItemFinanceDetails.setBillTaxRate(new BigDecimal("8.00")); // 8% tax
        
        // Calculate line totals using null-safe operations
        BigDecimal quantity = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getQuantity());
        BigDecimal rate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineGrossRate());
        BigDecimal discount = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineDiscountRate());
        BigDecimal taxRate = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getBillTaxRate());
        
        // Calculate gross total: quantity * rate
        BigDecimal grossTotal = BigDecimalUtil.multiply(quantity, rate);
        assertEquals(0, new BigDecimal("255.00").compareTo(grossTotal));
        
        // Calculate discount amount (should be zero since discount rate is null)
        BigDecimal discountAmount = BigDecimalUtil.multiply(grossTotal, discount.divide(BigDecimal.valueOf(100)));
        assertEquals(0, BigDecimal.ZERO.compareTo(discountAmount));
        
        // Calculate net total after discount
        BigDecimal netTotal = BigDecimalUtil.subtract(grossTotal, discountAmount);
        assertEquals(0, new BigDecimal("255.00").compareTo(netTotal));
        
        // Store calculated values
        billItemFinanceDetails.setGrossTotal(grossTotal);
        billItemFinanceDetails.setNetTotal(netTotal);
        
        // Verify stored values
        assertEquals(0, new BigDecimal("255.00").compareTo(billItemFinanceDetails.getGrossTotal()));
        assertEquals(0, new BigDecimal("255.00").compareTo(billItemFinanceDetails.getNetTotal()));
    }
    
    @Test
    @DisplayName("Bill Finance Details Aggregation with Null Values")
    public void testBillFinanceDetailsAggregation_WithNullValues_ShouldHandleCorrectly() {
        // Test aggregating multiple line items into bill-level totals
        
        // Line Item 1: Complete data
        BigDecimal item1Gross = new BigDecimal("100.00");
        BigDecimal item1Net = new BigDecimal("95.00");
        
        // Line Item 2: Partial data (some nulls)
        BigDecimal item2Gross = new BigDecimal("50.00");
        BigDecimal item2Net = null; // No net total calculated yet
        
        // Line Item 3: All nulls (e.g., placeholder item)
        BigDecimal item3Gross = null;
        BigDecimal item3Net = null;
        
        // Aggregate using null-safe operations
        BigDecimal totalGross = BigDecimalUtil.add(
            BigDecimalUtil.add(item1Gross, item2Gross),
            item3Gross
        );
        
        BigDecimal totalNet = BigDecimalUtil.add(
            BigDecimalUtil.add(item1Net, item2Net),
            item3Net
        );
        
        // Verify aggregated totals
        assertEquals(new BigDecimal("150.00"), totalGross); // 100 + 50 + 0
        assertEquals(new BigDecimal("95.00"), totalNet);    // 95 + 0 + 0
        
        // Store in bill finance details
        billFinanceDetails.setGrossTotal(totalGross);
        billFinanceDetails.setNetTotal(totalNet);
        
        // Verify persistence
        assertEquals(new BigDecimal("150.00"), billFinanceDetails.getGrossTotal());
        assertEquals(new BigDecimal("95.00"), billFinanceDetails.getNetTotal());
    }
    
    @Test
    @DisplayName("Entity Clone Operation with Null Values")
    public void testEntityClone_WithNullValues_ShouldPreserveNulls() {
        // Set up entity with mixed null and non-null values
        billItemFinanceDetails.setQuantity(new BigDecimal("5.00"));
        billItemFinanceDetails.setLineGrossRate(null);
        billItemFinanceDetails.setNetTotal(new BigDecimal("25.99"));
        billItemFinanceDetails.setBillDiscount(null);
        
        // Clone the entity
        BillItemFinanceDetails cloned = billItemFinanceDetails.clone();
        
        // Verify that null values are preserved in clone
        assertEquals(new BigDecimal("5.00"), cloned.getQuantity());
        assertNull(cloned.getLineGrossRate());
        assertEquals(new BigDecimal("25.99"), cloned.getNetTotal());
        assertNull(cloned.getBillDiscount());
        
        // Verify that unitsPerPack default is preserved
        assertEquals(BigDecimal.ONE, cloned.getUnitsPerPack());
    }
    
    @Test
    @DisplayName("Pharmacy Purchase Workflow Integration Test")
    public void testPharmacyPurchaseWorkflow_WithNullableFields_ShouldWorkCorrectly() {
        // Simulate a complete pharmacy purchase workflow
        
        // Step 1: Create bill item for purchase
        billItemFinanceDetails.setQuantity(new BigDecimal("20.00"));
        billItemFinanceDetails.setFreeQuantity(null); // No free quantity
        billItemFinanceDetails.setLineGrossRate(new BigDecimal("15.75"));
        billItemFinanceDetails.setLineDiscountRate(new BigDecimal("5.00")); // 5% discount
        billItemFinanceDetails.setBillTaxRate(new BigDecimal("10.00")); // 10% tax
        
        // Step 2: Calculate quantities (including null-safe operations)
        BigDecimal totalQuantity = BigDecimalUtil.add(
            billItemFinanceDetails.getQuantity(),
            billItemFinanceDetails.getFreeQuantity()
        );
        assertEquals(new BigDecimal("20.00"), totalQuantity); // 20 + 0 = 20
        
        // Step 3: Calculate financial totals
        BigDecimal grossTotal = BigDecimalUtil.multiply(
            billItemFinanceDetails.getQuantity(),
            billItemFinanceDetails.getLineGrossRate()
        );
        assertEquals(0, new BigDecimal("315.00").compareTo(grossTotal)); // 20 * 15.75
        
        // Step 4: Apply discount
        BigDecimal discountAmount = BigDecimalUtil.multiply(
            grossTotal,
            billItemFinanceDetails.getLineDiscountRate().divide(BigDecimal.valueOf(100))
        );
        assertEquals(0, new BigDecimal("15.75").compareTo(discountAmount)); // 315 * 0.05
        
        BigDecimal netTotal = BigDecimalUtil.subtract(grossTotal, discountAmount);
        assertEquals(0, new BigDecimal("299.25").compareTo(netTotal)); // 315 - 15.75
        
        // Step 5: Store calculated values
        billItemFinanceDetails.setTotalQuantity(totalQuantity);
        billItemFinanceDetails.setGrossTotal(grossTotal);
        billItemFinanceDetails.setLineDiscount(discountAmount);
        billItemFinanceDetails.setNetTotal(netTotal);
        
        // Step 6: Verify all calculations are stored correctly
        assertEquals(new BigDecimal("20.00"), billItemFinanceDetails.getTotalQuantity());
        assertEquals(0, new BigDecimal("315.00").compareTo(billItemFinanceDetails.getGrossTotal()));
        assertEquals(0, new BigDecimal("15.75").compareTo(billItemFinanceDetails.getLineDiscount()));
        assertEquals(0, new BigDecimal("299.25").compareTo(billItemFinanceDetails.getNetTotal()));
    }
    
    @Test
    @DisplayName("All-Null Scenario Edge Case")
    public void testAllNullScenario_ShouldHandleGracefully() {
        // Test scenario where all BigDecimal fields are null
        
        // Perform calculations with all null inputs
        BigDecimal sum = BigDecimalUtil.add(null, null);
        BigDecimal difference = BigDecimalUtil.subtract(null, null);
        BigDecimal product = BigDecimalUtil.multiply(null, null);
        
        // All should result in zero
        assertEquals(BigDecimal.ZERO, sum);
        assertEquals(BigDecimal.ZERO, difference);
        assertEquals(BigDecimal.ZERO, product);
        
        // Validation methods should handle nulls appropriately
        assertTrue(BigDecimalUtil.isNullOrZero(null));
        assertFalse(BigDecimalUtil.isPositive(null));
        assertFalse(BigDecimalUtil.isNegative(null));
    }
    
    @Test
    @DisplayName("Data Migration Compatibility Test")
    public void testDataMigrationCompatibility_ExistingZeroValues_ShouldWorkCorrectly() {
        // Test that existing zero values (from before migration) work correctly
        // with new null-safe operations
        
        // Simulate existing data with explicit zero values
        billItemFinanceDetails.setQuantity(BigDecimal.ZERO);
        billItemFinanceDetails.setLineGrossRate(BigDecimal.ZERO);
        billItemFinanceDetails.setNetTotal(BigDecimal.ZERO);
        
        // Operations with zero values should work identically to null values
        BigDecimal result1 = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getQuantity());
        BigDecimal result2 = BigDecimalUtil.valueOrZero(billItemFinanceDetails.getLineGrossRate());
        
        assertEquals(BigDecimal.ZERO, result1);
        assertEquals(BigDecimal.ZERO, result2);
        
        // Validation methods should treat zeros appropriately
        assertTrue(BigDecimalUtil.isNullOrZero(billItemFinanceDetails.getQuantity()));
        assertFalse(BigDecimalUtil.isPositive(billItemFinanceDetails.getLineGrossRate()));
        
        // Arithmetic operations with zeros should produce expected results
        BigDecimal sum = BigDecimalUtil.add(billItemFinanceDetails.getQuantity(), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), sum);
    }
}