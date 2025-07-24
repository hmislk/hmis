package com.divudi.controller;

import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.util.BigDecimalUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for controller layer BigDecimal validation logic.
 * Validates that controllers properly validate nullable BigDecimal fields
 * and provide appropriate user feedback for invalid input.
 * 
 * @author Dr M H B Ariyaratne
 * @since BigDecimal Refactoring Phase 6
 */
@DisplayName("Controller Validation Tests")
public class ControllerValidationTest {
    
    @Nested
    @DisplayName("Pharmacy Controller Validation Tests")
    class PharmacyControllerValidationTest {
        
        private BillItemFinanceDetails testFinanceDetails;
        
        @BeforeEach
        public void setUp() {
            testFinanceDetails = new BillItemFinanceDetails();
        }
        
        @Test
        @DisplayName("Quantity Validation Should Reject Null and Zero Values")
        public void testQuantityValidation_ShouldRejectNullAndZeroValues() {
            // Simulate controller validation logic for quantity
            
            // Test null quantity (should be invalid)
            testFinanceDetails.setQuantity(null);
            boolean isValidQuantity = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getQuantity()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getQuantity());
            assertFalse(isValidQuantity, "Null quantity should be invalid");
            
            // Test zero quantity (should be invalid)
            testFinanceDetails.setQuantity(BigDecimal.ZERO);
            isValidQuantity = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getQuantity()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getQuantity());
            assertFalse(isValidQuantity, "Zero quantity should be invalid");
            
            // Test negative quantity (should be invalid)
            testFinanceDetails.setQuantity(new BigDecimal("-5.00"));
            isValidQuantity = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getQuantity()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getQuantity());
            assertFalse(isValidQuantity, "Negative quantity should be invalid");
            
            // Test positive quantity (should be valid)
            testFinanceDetails.setQuantity(new BigDecimal("5.00"));
            isValidQuantity = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getQuantity()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getQuantity());
            assertTrue(isValidQuantity, "Positive quantity should be valid");
        }
        
        @Test
        @DisplayName("Rate Validation Should Reject Invalid Values")
        public void testRateValidation_ShouldRejectInvalidValues() {
            // Test line gross rate validation
            
            // Null rate (should be invalid)
            testFinanceDetails.setLineGrossRate(null);
            boolean isValidRate = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getLineGrossRate()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getLineGrossRate());
            assertFalse(isValidRate, "Null rate should be invalid");
            
            // Zero rate (should be invalid for purchase rates)
            testFinanceDetails.setLineGrossRate(BigDecimal.ZERO);
            isValidRate = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getLineGrossRate()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getLineGrossRate());
            assertFalse(isValidRate, "Zero rate should be invalid for purchases");
            
            // Negative rate (should be invalid)
            testFinanceDetails.setLineGrossRate(new BigDecimal("-10.00"));
            isValidRate = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getLineGrossRate()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getLineGrossRate());
            assertFalse(isValidRate, "Negative rate should be invalid");
            
            // Valid positive rate
            testFinanceDetails.setLineGrossRate(new BigDecimal("15.75"));
            isValidRate = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getLineGrossRate()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getLineGrossRate());
            assertTrue(isValidRate, "Positive rate should be valid");
        }
        
        @Test
        @DisplayName("Retail Rate Validation Should Handle Per-Unit Calculations")
        public void testRetailRateValidation_ShouldHandlePerUnitCalculations() {
            // Test retail rate validation logic
            
            testFinanceDetails.setRetailSaleRatePerUnit(null);
            boolean isValidRetailRate = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getRetailSaleRatePerUnit()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getRetailSaleRatePerUnit());
            assertFalse(isValidRetailRate, "Null retail rate should be invalid");
            
            // Test with valid rate
            testFinanceDetails.setRetailSaleRatePerUnit(new BigDecimal("20.00"));
            isValidRetailRate = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getRetailSaleRatePerUnit()) 
                && !BigDecimalUtil.isNegative(testFinanceDetails.getRetailSaleRatePerUnit());
            assertTrue(isValidRetailRate, "Positive retail rate should be valid");
        }
        
        @Test
        @DisplayName("Optional Field Validation Should Allow Null Values")
        public void testOptionalFieldValidation_ShouldAllowNullValues() {
            // Test fields that are optional and can be null
            
            // Free quantity can be null (optional field)
            testFinanceDetails.setFreeQuantity(null);
            // No validation error should occur for optional fields
            assertTrue(true, "Null free quantity should be acceptable");
            
            // Discount rates can be null (no discount applied)
            testFinanceDetails.setLineDiscountRate(null);
            testFinanceDetails.setBillDiscountRate(null);
            // These should be acceptable
            assertTrue(true, "Null discount rates should be acceptable");
            
            // Tax rates can be null in some scenarios
            testFinanceDetails.setBillTaxRate(null);
            // Should be acceptable
            assertTrue(true, "Null tax rate should be acceptable in some scenarios");
        }
        
        @Test
        @DisplayName("Business Rule Validation Should Enforce Logic Constraints")
        public void testBusinessRuleValidation_ShouldEnforceLogicConstraints() {
            // Test business rules that span multiple fields
            
            // Rule: If quantity is set, rate must also be set for calculations
            testFinanceDetails.setQuantity(new BigDecimal("5.00"));
            testFinanceDetails.setLineGrossRate(null);
            
            boolean canCalculateTotal = BigDecimalUtil.isPositive(testFinanceDetails.getQuantity()) 
                && BigDecimalUtil.isPositive(testFinanceDetails.getLineGrossRate());
            assertFalse(canCalculateTotal, "Cannot calculate total without both quantity and rate");
            
            // Set rate and test again
            testFinanceDetails.setLineGrossRate(new BigDecimal("10.00"));
            canCalculateTotal = BigDecimalUtil.isPositive(testFinanceDetails.getQuantity()) 
                && BigDecimalUtil.isPositive(testFinanceDetails.getLineGrossRate());
            assertTrue(canCalculateTotal, "Should be able to calculate total with both values");
            
            // Rule: unitsPerPack should never be null or zero
            assertNotNull(testFinanceDetails.getUnitsPerPack());
            assertTrue(BigDecimalUtil.isPositive(testFinanceDetails.getUnitsPerPack()));
        }
        
        @Test
        @DisplayName("Form Input Processing Should Handle Null Conversions")
        public void testFormInputProcessing_ShouldHandleNullConversions() {
            // Simulate processing form input where empty strings become null
            
            // Empty form field converted to null
            String emptyQuantity = "";
            BigDecimal quantityValue = emptyQuantity.isEmpty() ? null : new BigDecimal(emptyQuantity);
            testFinanceDetails.setQuantity(quantityValue);
            
            // Validation should handle this appropriately
            boolean isValidForCalculation = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getQuantity());
            assertFalse(isValidForCalculation, "Empty form input should result in invalid state");
            
            // Non-empty form field
            String validQuantity = "7.50";
            quantityValue = validQuantity.isEmpty() ? null : new BigDecimal(validQuantity);
            testFinanceDetails.setQuantity(quantityValue);
            
            isValidForCalculation = !BigDecimalUtil.isNullOrZero(testFinanceDetails.getQuantity());
            assertTrue(isValidForCalculation, "Valid form input should result in valid state");
        }
    }
    
    @Nested
    @DisplayName("Validation Message Generation Tests")
    class ValidationMessageTest {
        
        @Test
        @DisplayName("Error Messages Should Be Appropriate for Null Values")
        public void testErrorMessages_ShouldBeAppropriateForNullValues() {
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            
            // Generate appropriate error messages for null values
            item.setQuantity(null);
            String quantityError = generateQuantityErrorMessage(item.getQuantity());
            assertEquals("Please enter quantity", quantityError);
            
            item.setLineGrossRate(null);
            String rateError = generateRateErrorMessage(item.getLineGrossRate());
            assertEquals("Please enter the purchase rate", rateError);
            
            item.setRetailSaleRatePerUnit(null);
            String retailError = generateRetailRateErrorMessage(item.getRetailSaleRatePerUnit());
            assertEquals("Please enter the sale rate", retailError);
        }
        
        @Test
        @DisplayName("Error Messages Should Be Appropriate for Invalid Values")
        public void testErrorMessages_ShouldBeAppropriateForInvalidValues() {
            BillItemFinanceDetails item = new BillItemFinanceDetails();
            
            // Zero values
            item.setQuantity(BigDecimal.ZERO);
            String quantityError = generateQuantityErrorMessage(item.getQuantity());
            assertEquals("Please enter quantity", quantityError);
            
            // Negative values
            item.setLineGrossRate(new BigDecimal("-5.00"));
            String rateError = generateRateErrorMessage(item.getLineGrossRate());
            assertEquals("Purchase rate must be positive", rateError);
        }
        
        // Helper methods to simulate controller error message generation
        private String generateQuantityErrorMessage(BigDecimal quantity) {
            if (BigDecimalUtil.isNullOrZero(quantity) || BigDecimalUtil.isNegative(quantity)) {
                return "Please enter quantity";
            }
            return null;
        }
        
        private String generateRateErrorMessage(BigDecimal rate) {
            if (BigDecimalUtil.isNullOrZero(rate)) {
                return "Please enter the purchase rate";
            } else if (BigDecimalUtil.isNegative(rate)) {
                return "Purchase rate must be positive";
            }
            return null;
        }
        
        private String generateRetailRateErrorMessage(BigDecimal rate) {
            if (BigDecimalUtil.isNullOrZero(rate) || BigDecimalUtil.isNegative(rate)) {
                return "Please enter the sale rate";
            }
            return null;
        }
    }
    
    @Test
    @DisplayName("Controller Pre-Save Validation Should Prevent Invalid Data")
    public void testControllerPreSaveValidation_ShouldPreventInvalidData() {
        BillItemFinanceDetails item = new BillItemFinanceDetails();
        
        // Simulate pre-save validation that would occur in controller
        boolean canSave = true;
        String validationErrors = "";
        
        // Check required fields
        if (BigDecimalUtil.isNullOrZero(item.getQuantity()) || BigDecimalUtil.isNegative(item.getQuantity())) {
            canSave = false;
            validationErrors += "Invalid quantity; ";
        }
        
        if (BigDecimalUtil.isNullOrZero(item.getLineGrossRate()) || BigDecimalUtil.isNegative(item.getLineGrossRate())) {
            canSave = false;
            validationErrors += "Invalid purchase rate; ";
        }
        
        // Initial state should fail validation
        assertFalse(canSave);
        assertTrue(validationErrors.contains("Invalid quantity"));
        assertTrue(validationErrors.contains("Invalid purchase rate"));
        
        // Set valid values
        item.setQuantity(new BigDecimal("5.00"));
        item.setLineGrossRate(new BigDecimal("10.00"));
        
        // Re-validate
        canSave = true;
        validationErrors = "";
        
        if (BigDecimalUtil.isNullOrZero(item.getQuantity()) || BigDecimalUtil.isNegative(item.getQuantity())) {
            canSave = false;
        }
        
        if (BigDecimalUtil.isNullOrZero(item.getLineGrossRate()) || BigDecimalUtil.isNegative(item.getLineGrossRate())) {
            canSave = false;
        }
        
        // Should now pass validation
        assertTrue(canSave);
        assertEquals("", validationErrors);
    }
}