/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.core.entity.Item;
import com.divudi.core.entity.clinical.Prescription;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.facade.ItemFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * EJB Service for converting Prescription to appropriate Item and Quantity
 * Handles the logic of determining which specific item to dispense and in what quantity
 * based on the prescription details
 * 
 * @author buddhika
 */
@Stateless
public class PrescriptionToItemService {

    @EJB
    private ItemFacade itemFacade;
    
    @EJB
    private PrescriptionService prescriptionService;

    /**
     * Result class to hold both item and calculated quantity
     */
    public static class PrescriptionToItemResult {
        private Item item;
        private Double quantity;
        private String calculationNote;
        private boolean success;
        private String errorMessage;

        public PrescriptionToItemResult(Item item, Double quantity, String calculationNote) {
            this.item = item;
            this.quantity = quantity;
            this.calculationNote = calculationNote;
            this.success = true;
        }

        public PrescriptionToItemResult(String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
        }

        // Getters and setters
        public Item getItem() { return item; }
        public void setItem(Item item) { this.item = item; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public String getCalculationNote() { return calculationNote; }
        public void setCalculationNote(String calculationNote) { this.calculationNote = calculationNote; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Main method to convert prescription to item and quantity
     * @param prescription The prescription containing medicine details
     * @return Result containing the item and calculated quantity
     */
    public PrescriptionToItemResult calculateItemAndQuantity(Prescription prescription) {
        if (prescription == null) {
            return new PrescriptionToItemResult("Prescription cannot be null");
        }

        Item prescribedItem = prescription.getItem();
        if (prescribedItem == null) {
            return new PrescriptionToItemResult("No medicine selected in prescription");
        }

        // Handle different types of prescribed items
        if (prescribedItem instanceof Amp) {
            return calculateFromAmp(prescription, (Amp) prescribedItem);
        } else if (prescribedItem instanceof Vmp) {
            return calculateFromVmp(prescription, (Vmp) prescribedItem);
        } else {
            // For VTM or ATM, try to find suitable AMP/VMP
            return findSuitableItemFromGeneric(prescription, prescribedItem);
        }
    }

    /**
     * Calculate item and quantity when prescription is given as AMP
     * @param prescription The prescription
     * @param amp The prescribed AMP
     * @return Calculation result
     */
    private PrescriptionToItemResult calculateFromAmp(Prescription prescription, Amp amp) {
        try {
            // Get prescription details
            Double dose = prescription.getDose();
            MeasurementUnit doseUnit = prescription.getDoseUnit();
            MeasurementUnit frequencyUnit = prescription.getFrequencyUnit();
            Double duration = prescription.getDuration();
            MeasurementUnit durationUnit = prescription.getDurationUnit();
            MeasurementUnit issueUnit = prescription.getIssueUnit();

            if (dose == null || frequencyUnit == null || duration == null || durationUnit == null) {
                return new PrescriptionToItemResult("Incomplete prescription: dose, frequency, duration and duration unit are required");
            }

            // Calculate total quantity needed
            Double totalQuantity = calculateTotalQuantity(dose, doseUnit, frequencyUnit, duration, durationUnit, issueUnit, amp);
            
            if (totalQuantity == null || totalQuantity <= 0) {
                return new PrescriptionToItemResult("Could not calculate valid quantity from prescription");
            }

            String calculationNote = String.format("AMP calculation: %.2f %s × %s × %.1f %s = %.2f units", 
                dose, 
                doseUnit != null ? doseUnit.getName() : "units",
                frequencyUnit.getName(),
                duration,
                durationUnit != null ? durationUnit.getName() : "days",
                totalQuantity);

            return new PrescriptionToItemResult(amp, totalQuantity, calculationNote);

        } catch (Exception e) {
            return new PrescriptionToItemResult("Error calculating from AMP: " + e.getMessage());
        }
    }

    /**
     * Calculate item and quantity when prescription is given as VMP
     * @param prescription The prescription
     * @param vmp The prescribed VMP
     * @return Calculation result
     */
    private PrescriptionToItemResult calculateFromVmp(Prescription prescription, Vmp vmp) {
        try {
            // For VMP, try to find the best matching AMP
            List<Item> availableAmps = findAmpsForVmp(vmp);
            
            if (availableAmps.isEmpty()) {
                // If no AMPs found, use the VMP itself
                Double totalQuantity = calculateTotalQuantityForVmp(prescription, vmp);
                String calculationNote = "VMP calculation (no specific AMP found)";
                return new PrescriptionToItemResult(vmp, totalQuantity, calculationNote);
            }

            // Use the first available AMP (could be enhanced with better selection logic)
            Item selectedAmp = availableAmps.get(0);
            if (selectedAmp instanceof Amp) {
                // Create a temporary prescription with the AMP and recalculate
                Prescription tempPrescription = copyPrescriptionWithItem(prescription, selectedAmp);
                return calculateFromAmp(tempPrescription, (Amp) selectedAmp);
            }

            return new PrescriptionToItemResult("No suitable AMP found for VMP");

        } catch (Exception e) {
            return new PrescriptionToItemResult("Error calculating from VMP: " + e.getMessage());
        }
    }

    /**
     * Find suitable item when prescription is given as VTM or ATM
     * @param prescription The prescription
     * @param genericItem The generic item (VTM/ATM)
     * @return Calculation result
     */
    private PrescriptionToItemResult findSuitableItemFromGeneric(Prescription prescription, Item genericItem) {
        try {
            // Find VMPs or AMPs related to this generic item
            List<Item> suitableItems = findItemsForGeneric(genericItem, prescription);
            
            if (suitableItems.isEmpty()) {
                return new PrescriptionToItemResult("No suitable specific items found for " + genericItem.getName());
            }

            // Use the first suitable item
            Item selectedItem = suitableItems.get(0);
            
            if (selectedItem instanceof Amp) {
                Prescription tempPrescription = copyPrescriptionWithItem(prescription, selectedItem);
                return calculateFromAmp(tempPrescription, (Amp) selectedItem);
            } else if (selectedItem instanceof Vmp) {
                Prescription tempPrescription = copyPrescriptionWithItem(prescription, selectedItem);
                return calculateFromVmp(tempPrescription, (Vmp) selectedItem);
            }

            return new PrescriptionToItemResult("Selected item is not AMP or VMP");

        } catch (Exception e) {
            return new PrescriptionToItemResult("Error finding suitable item: " + e.getMessage());
        }
    }

    /**
     * Calculate total quantity needed based on prescription parameters
     * This is the core calculation for AMP with strength in issue unit
     */
    private Double calculateTotalQuantity(Double dose, MeasurementUnit doseUnit, 
                                        MeasurementUnit frequencyUnit, Double duration, 
                                        MeasurementUnit durationUnit, MeasurementUnit issueUnit, 
                                        Amp amp) {
        
        // Convert duration to days
        Double durationInDays = prescriptionService.convertDurationToDays(duration, durationUnit);
        if (durationInDays == null || durationInDays <= 0) {
            return null;
        }

        // Calculate doses per day from frequency
        Double dosesPerDay = calculateDosesPerDay(frequencyUnit);
        if (dosesPerDay == null || dosesPerDay <= 0) {
            return null;
        }

        // For AMP with strength in issue unit case:
        // Total quantity = dose × doses per day × number of days
        Double totalQuantity = dose * dosesPerDay * durationInDays;

        return totalQuantity;
    }

    /**
     * Calculate doses per day from frequency unit
     */
    private Double calculateDosesPerDay(MeasurementUnit frequencyUnit) {
        if (frequencyUnit == null) {
            return 1.0; // Default to once per day
        }
        String frequencyName = Optional.ofNullable(frequencyUnit.getName())
                .orElse("")
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");

        if (frequencyName.isEmpty()) {
            return 1.0;
        }
        
        // Common frequency mappings
        Map<String, Double> frequencyMap = new HashMap<>();
        frequencyMap.put("once daily", 1.0);
        frequencyMap.put("od", 1.0);
        frequencyMap.put("twice daily", 2.0);
        frequencyMap.put("bd", 2.0);
        frequencyMap.put("bid", 2.0);
        frequencyMap.put("three times daily", 3.0);
        frequencyMap.put("tds", 3.0);
        frequencyMap.put("tid", 3.0);
        frequencyMap.put("four times daily", 4.0);
        frequencyMap.put("qds", 4.0);
        frequencyMap.put("qid", 4.0);
        frequencyMap.put("every 6 hours", 4.0);
        frequencyMap.put("every 8 hours", 3.0);
        frequencyMap.put("every 12 hours", 2.0);
        frequencyMap.put("every 24 hours", 1.0);
        frequencyMap.put("prn", 1.0); // As needed - default to once

        return frequencyMap.getOrDefault(frequencyName, 1.0);
    }

    /**
     * Calculate total quantity for VMP (placeholder implementation)
     */
    private Double calculateTotalQuantityForVmp(Prescription prescription, Vmp vmp) {
        // Simplified calculation for VMP
        // This would need more sophisticated logic based on VMP properties
        return 30.0; // Default quantity
    }

    /**
     * Find AMPs associated with a VMP
     */
    private List<Item> findAmpsForVmp(Vmp vmp) {
        try {
            String jpql = "SELECT a FROM Amp a WHERE a.vmp = :vmp AND a.retired = false";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("vmp", vmp);
            return itemFacade.findByJpql(jpql, parameters);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Find suitable items for generic prescription (VTM/ATM)
     */
    private List<Item> findItemsForGeneric(Item genericItem, Prescription prescription) {
        try {
            // This is a simplified implementation
            // In practice, this would involve complex matching based on:
            // - Strength requirements
            // - Dosage form preferences
            // - Availability
            String jpql = "SELECT i FROM Item i WHERE i.name LIKE :pattern AND i.retired = false";
            String pattern = "%" + genericItem.getName() + "%";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("pattern", pattern);
            return itemFacade.findByJpql(jpql, parameters);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Create a copy of prescription with different item
     */
    private Prescription copyPrescriptionWithItem(Prescription original, Item newItem) {
        Prescription copy = new Prescription();
        copy.setItem(newItem);
        copy.setDose(original.getDose());
        copy.setDoseUnit(original.getDoseUnit());
        copy.setFrequencyUnit(original.getFrequencyUnit());
        copy.setDuration(original.getDuration());
        copy.setDurationUnit(original.getDurationUnit());
        copy.setIssueUnit(original.getIssueUnit());
        copy.setPrescribedFrom(original.getPrescribedFrom());
        copy.setPrescribedTo(original.getPrescribedTo());
        return copy;
    }

    /**
     * Validate if prescription has minimum required information for calculation
     */
    public boolean isCalculationPossible(Prescription prescription) {
        if (prescription == null || prescription.getItem() == null) {
            return false;
        }

        // Check for minimum required fields
        return prescription.getDose() != null && 
               prescription.getFrequencyUnit() != null && 
               prescription.getDuration() != null &&
               prescription.getDurationUnit() != null;
    }

    /**
     * Get detailed calculation explanation for UI display
     */
    public String getCalculationExplanation(Prescription prescription) {
        if (!isCalculationPossible(prescription)) {
            return "Insufficient data for calculation";
        }

        PrescriptionToItemResult result = calculateItemAndQuantity(prescription);
        if (result.isSuccess()) {
            return result.getCalculationNote();
        } else {
            return result.getErrorMessage();
        }
    }
}