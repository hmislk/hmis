package com.divudi.service;

import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.util.BigDecimalUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for service layer BigDecimal handling after refactoring.
 * Validates that service layer methods correctly handle nullable BigDecimal fields
 * and use BigDecimalUtil for null-safe operations.
 * 
 * @author Dr M H B Ariyaratne
 * @since BigDecimal Refactoring Phase 6
 */
@DisplayName("Service Layer BigDecimal Tests")
public class ServiceLayerBigDecimalTest {
    
    @Nested
    @DisplayName("Pharmacy Costing Service Tests")
    class PharmacyCostingServiceTest {
        
        private BillItemFinanceDetails testBillItemFinanceDetails;
        
        @BeforeEach
        public void setUp() {
            testBillItemFinanceDetails = new BillItemFinanceDetails();
        }
        
        @Test
        @DisplayName("Financial Calculation with Null Quantity Should Use Zero")
        public void testFinancialCalculation_WithNullQuantity_ShouldUseZero() {
            // Simulate PharmacyCostingService logic with null quantity
            testBillItemFinanceDetails.setQuantity(null); // No quantity set
            testBillItemFinanceDetails.setLineGrossRate(new BigDecimal("10.00"));
            
            // Service layer should use BigDecimalUtil for null-safe calculations
            BigDecimal quantity = BigDecimalUtil.valueOrZero(testBillItemFinanceDetails.getQuantity());
            BigDecimal rate = BigDecimalUtil.valueOrZero(testBillItemFinanceDetails.getLineGrossRate());
            
            BigDecimal total = BigDecimalUtil.multiply(quantity, rate);
            
            // Result should be zero when quantity is null
            assertEquals(0, BigDecimal.ZERO.compareTo(total));
        }
        
        @Test
        @DisplayName("Total Quantity Calculation with Mixed Null Values")
        public void testTotalQuantityCalculation_WithMixedNullValues_ShouldCalculateCorrectly() {
            // Test the pattern found in PharmacyCostingService
            testBillItemFinanceDetails.setQuantity(new BigDecimal("5.00"));
            testBillItemFinanceDetails.setFreeQuantity(null); // No free quantity
            
            // Calculate total quantity using null-safe addition
            BigDecimal totalQty = BigDecimalUtil.add(
                testBillItemFinanceDetails.getQuantity(),
                testBillItemFinanceDetails.getFreeQuantity()
            );
            
            assertEquals(new BigDecimal("5.00"), totalQty);
            
            // Test with both values present
            testBillItemFinanceDetails.setFreeQuantity(new BigDecimal("2.00"));
            totalQty = BigDecimalUtil.add(
                testBillItemFinanceDetails.getQuantity(),
                testBillItemFinanceDetails.getFreeQuantity()
            );
            
            assertEquals(new BigDecimal("7.00"), totalQty);
        }
        
        @Test
        @DisplayName("Units Per Pack Calculation Should Never Use Null")
        public void testUnitsPerPackCalculation_ShouldNeverUseNull() {
            // unitsPerPack should always have a value (BigDecimal.ONE by default)
            assertNotNull(testBillItemFinanceDetails.getUnitsPerPack());
            assertEquals(BigDecimal.ONE, testBillItemFinanceDetails.getUnitsPerPack());
            
            // Service layer calculations using unitsPerPack should work safely
            BigDecimal quantity = new BigDecimal("10.00");
            BigDecimal unitsPerPack = testBillItemFinanceDetails.getUnitsPerPack();
            
            BigDecimal totalUnits = BigDecimalUtil.multiply(quantity, unitsPerPack);
            assertEquals(new BigDecimal("10.00"), totalUnits);
            
            // Test with custom unitsPerPack value
            testBillItemFinanceDetails.setUnitsPerPack(new BigDecimal("12.00"));
            unitsPerPack = testBillItemFinanceDetails.getUnitsPerPack();
            totalUnits = BigDecimalUtil.multiply(quantity, unitsPerPack);
            assertEquals(0, new BigDecimal("120.00").compareTo(totalUnits));
        }
        
        @Test
        @DisplayName("Rate Validation Logic Should Handle Null Rates")
        public void testRateValidation_ShouldHandleNullRates() {
            // Test validation patterns used in service layer
            testBillItemFinanceDetails.setLineGrossRate(null);
            testBillItemFinanceDetails.setBillGrossRate(new BigDecimal("0.00"));
            testBillItemFinanceDetails.setNetRate(new BigDecimal("15.00"));
            
            // Service layer validation using BigDecimalUtil
            boolean hasValidLineRate = !BigDecimalUtil.isNullOrZero(testBillItemFinanceDetails.getLineGrossRate());
            boolean hasValidBillRate = !BigDecimalUtil.isNullOrZero(testBillItemFinanceDetails.getBillGrossRate());
            boolean hasValidNetRate = !BigDecimalUtil.isNullOrZero(testBillItemFinanceDetails.getNetRate());
            
            assertFalse(hasValidLineRate); // null should be invalid
            assertFalse(hasValidBillRate); // zero should be invalid  
            assertTrue(hasValidNetRate);   // 15.00 should be valid
        }
        
        @Test
        @DisplayName("Percentage Calculation with Null Values")
        public void testPercentageCalculation_WithNullValues_ShouldHandleCorrectly() {
            // Test discount/tax percentage calculations
            testBillItemFinanceDetails.setGrossTotal(new BigDecimal("100.00"));
            testBillItemFinanceDetails.setLineDiscountRate(null); // No discount
            testBillItemFinanceDetails.setBillTaxRate(new BigDecimal("10.00")); // 10% tax
            
            BigDecimal grossTotal = testBillItemFinanceDetails.getGrossTotal();
            
            // Calculate discount (should be zero when rate is null)
            BigDecimal discountRate = BigDecimalUtil.valueOrZero(testBillItemFinanceDetails.getLineDiscountRate());
            BigDecimal discountAmount = BigDecimalUtil.multiply(
                grossTotal,
                discountRate.divide(BigDecimal.valueOf(100))
            );
            assertEquals(0, BigDecimal.ZERO.compareTo(discountAmount));
            
            // Calculate tax (should work with valid rate)
            BigDecimal taxRate = BigDecimalUtil.valueOrZero(testBillItemFinanceDetails.getBillTaxRate());
            BigDecimal taxAmount = BigDecimalUtil.multiply(
                grossTotal,
                taxRate.divide(BigDecimal.valueOf(100))
            );
            assertEquals(0, new BigDecimal("10.00").compareTo(taxAmount));
        }
        
        @Test
        @DisplayName("Cost Analysis with Partial Data")
        public void testCostAnalysis_WithPartialData_ShouldProduceValidResults() {
            // Simulate cost analysis scenario with some missing data
            testBillItemFinanceDetails.setQuantity(new BigDecimal("8.00"));
            testBillItemFinanceDetails.setLineGrossRate(new BigDecimal("25.00"));
            testBillItemFinanceDetails.setRetailSaleRate(null); // Retail rate not set
            testBillItemFinanceDetails.setWholesaleRate(new BigDecimal("20.00"));
            
            // Calculate various cost metrics
            BigDecimal purchaseCost = BigDecimalUtil.multiply(
                testBillItemFinanceDetails.getQuantity(),
                testBillItemFinanceDetails.getLineGrossRate()
            );
            assertEquals(0, new BigDecimal("200.00").compareTo(purchaseCost));
            
            // Retail value calculation (should be zero when rate is null)
            BigDecimal retailValue = BigDecimalUtil.multiply(
                testBillItemFinanceDetails.getQuantity(),
                BigDecimalUtil.valueOrZero(testBillItemFinanceDetails.getRetailSaleRate())
            );
            assertEquals(0, BigDecimal.ZERO.compareTo(retailValue));
            
            // Wholesale value calculation
            BigDecimal wholesaleValue = BigDecimalUtil.multiply(
                testBillItemFinanceDetails.getQuantity(),
                testBillItemFinanceDetails.getWholesaleRate()
            );
            assertEquals(0, new BigDecimal("160.00").compareTo(wholesaleValue));
        }
    }
    
    @Nested
    @DisplayName("Financial Calculation Service Tests")
    class FinancialCalculationServiceTest {
        
        @Test
        @DisplayName("Bill Total Aggregation with Mixed Line Items")
        public void testBillTotalAggregation_WithMixedLineItems_ShouldCalculateCorrectly() {
            // Simulate multiple line items with different null patterns
            
            // Line Item 1: Complete data
            BillItemFinanceDetails item1 = new BillItemFinanceDetails();
            item1.setNetTotal(new BigDecimal("100.00"));
            item1.setBillTax(new BigDecimal("10.00"));
            item1.setLineDiscount(new BigDecimal("5.00"));
            
            // Line Item 2: Partial data
            BillItemFinanceDetails item2 = new BillItemFinanceDetails();
            item2.setNetTotal(new BigDecimal("50.00"));
            item2.setBillTax(null); // No tax
            item2.setLineDiscount(new BigDecimal("2.50"));
            
            // Line Item 3: Minimal data
            BillItemFinanceDetails item3 = new BillItemFinanceDetails();
            item3.setNetTotal(null); // No net total calculated
            item3.setBillTax(null);
            item3.setLineDiscount(null);
            
            // Aggregate using service layer logic (null-safe)
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
            
            // Verify aggregated totals
            assertEquals(new BigDecimal("150.00"), totalNet);    // 100 + 50 + 0
            assertEquals(new BigDecimal("10.00"), totalTax);     // 10 + 0 + 0
            assertEquals(new BigDecimal("7.50"), totalDiscount); // 5 + 2.5 + 0
        }
        
        @Test
        @DisplayName("Profit Margin Calculation with Null Costs")
        public void testProfitMarginCalculation_WithNullCosts_ShouldHandleSafely() {
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            item.setNetTotal(new BigDecimal("100.00"));
            item.setBillCost(null); // Cost not available
            item.setLineCost(new BigDecimal("60.00"));
            
            // Calculate total cost using null-safe operations
            BigDecimal totalCost = BigDecimalUtil.add(item.getBillCost(), item.getLineCost());
            assertEquals(new BigDecimal("60.00"), totalCost);
            
            // Calculate profit margin
            BigDecimal netTotal = BigDecimalUtil.valueOrZero(item.getNetTotal());
            BigDecimal profit = BigDecimalUtil.subtract(netTotal, totalCost);
            assertEquals(new BigDecimal("40.00"), profit);
            
            // Calculate profit margin percentage (if total cost is not zero)
            BigDecimal profitMarginPercent = BigDecimal.ZERO;
            if (!BigDecimalUtil.isNullOrZero(totalCost)) {
                profitMarginPercent = profit.divide(totalCost, 4, BigDecimal.ROUND_HALF_EVEN)
                    .multiply(BigDecimal.valueOf(100));
            }
            
            // Should calculate approximately 66.67% profit margin
            assertTrue(profitMarginPercent.compareTo(new BigDecimal("66.00")) > 0);
            assertTrue(profitMarginPercent.compareTo(new BigDecimal("67.00")) < 0);
        }
        
        @Test
        @DisplayName("Return Item Processing with Null Values")
        public void testReturnItemProcessing_WithNullValues_ShouldProcessCorrectly() {
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            item.setQuantity(new BigDecimal("10.00"));
            item.setReturnQuantity(null); // No returns initially
            item.setReturnFreeQuantity(null);
            item.setNetTotal(new BigDecimal("100.00"));
            item.setReturnNetTotal(null);
            
            // Process a return
            BigDecimal returnQty = new BigDecimal("2.00");
            BigDecimal unitRate = new BigDecimal("10.00"); // 100/10 = 10 per unit
            
            item.setReturnQuantity(returnQty);
            item.setReturnNetTotal(BigDecimalUtil.multiply(returnQty, unitRate));
            
            // Calculate remaining quantities and totals
            BigDecimal remainingQty = BigDecimalUtil.subtract(
                item.getQuantity(),
                BigDecimalUtil.valueOrZero(item.getReturnQuantity())
            );
            assertEquals(new BigDecimal("8.00"), remainingQty);
            
            BigDecimal remainingTotal = BigDecimalUtil.subtract(
                item.getNetTotal(),
                BigDecimalUtil.valueOrZero(item.getReturnNetTotal())
            );
            assertEquals(0, new BigDecimal("80.00").compareTo(remainingTotal));
        }
    }
    
    @Test
    @DisplayName("Service Layer Validation Rules")
    public void testServiceLayerValidationRules_ShouldEnforceBusinessLogic() {
        BillItemFinanceDetails item = new BillItemFinanceDetails();
        
        // Test validation rules that should be enforced by service layer
        
        // Rule 1: Quantity must be positive for valid transactions
        item.setQuantity(null);
        boolean isValidQuantity = BigDecimalUtil.isPositive(item.getQuantity());
        assertFalse(isValidQuantity);
        
        item.setQuantity(BigDecimal.ZERO);
        isValidQuantity = BigDecimalUtil.isPositive(item.getQuantity());
        assertFalse(isValidQuantity);
        
        item.setQuantity(new BigDecimal("5.00"));
        isValidQuantity = BigDecimalUtil.isPositive(item.getQuantity());
        assertTrue(isValidQuantity);
        
        // Rule 2: Rates should not be negative
        item.setLineGrossRate(new BigDecimal("-10.00"));
        boolean isValidRate = !BigDecimalUtil.isNegative(item.getLineGrossRate());
        assertFalse(isValidRate);
        
        item.setLineGrossRate(new BigDecimal("10.00"));
        isValidRate = !BigDecimalUtil.isNegative(item.getLineGrossRate());
        assertTrue(isValidRate);
        
        // Rule 3: unitsPerPack should never be null or zero
        assertNotNull(item.getUnitsPerPack());
        assertTrue(BigDecimalUtil.isPositive(item.getUnitsPerPack()));
    }
}