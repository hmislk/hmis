/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.core.entity.clinical.Prescription;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.ejb.Stateless;

/**
 * EJB Service for Prescription-related calculations and business logic
 * Handles date and duration calculations for prescriptions
 * 
 * @author buddhika
 */
@Stateless
public class PrescriptionService {

    /**
     * Calculates duration in days between prescribedFrom and prescribedTo dates
     * @param prescribedFrom Start date of prescription
     * @param prescribedTo End date of prescription
     * @return Duration in days, or null if either date is null
     */
    public Double calculateDurationFromDates(Date prescribedFrom, Date prescribedTo) {
        if (prescribedFrom == null || prescribedTo == null) {
            return null;
        }
        
        // Convert Date to LocalDate for easier calculation
        LocalDate fromDate = prescribedFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate toDate = prescribedTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        // Calculate days between dates
        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
        
        // Add 1 to include both start and end dates
        return (double) (daysBetween + 1);
    }

    /**
     * Calculates duration based on dates and duration unit
     * @param prescribedFrom Start date
     * @param prescribedTo End date
     * @param durationUnit Unit for duration calculation
     * @return Duration in the specified unit
     */
    public Double calculateDurationFromDates(Date prescribedFrom, Date prescribedTo, MeasurementUnit durationUnit) {
        if (prescribedFrom == null || prescribedTo == null || durationUnit == null) {
            return null;
        }
        
        Double daysCount = calculateDurationFromDates(prescribedFrom, prescribedTo);
        if (daysCount == null) {
            return null;
        }
        
        return convertDurationToUnit(daysCount, durationUnit);
    }

    /**
     * Calculates prescribedTo date based on prescribedFrom and duration
     * @param prescribedFrom Start date
     * @param duration Duration value
     * @param durationUnit Duration unit (days, weeks, months, etc.)
     * @return Calculated end date
     */
    public Date calculateToDateFromDuration(Date prescribedFrom, Double duration, MeasurementUnit durationUnit) {
        if (prescribedFrom == null || duration == null || duration <= 0) {
            return null;
        }
        
        // Convert duration to days
        Double durationInDays = convertDurationToDays(duration, durationUnit);
        if (durationInDays == null || durationInDays <= 0) {
            return null;
        }
        
        // Convert Date to LocalDate
        LocalDate fromDate = prescribedFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        // Add duration (subtract 1 because we include the start date)
        LocalDate toDate = fromDate.plusDays(durationInDays.longValue() - 1);
        
        // Convert back to Date
        return Date.from(toDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts duration from days to specified unit
     * @param durationInDays Duration in days
     * @param targetUnit Target measurement unit
     * @return Duration in target unit
     */
    public Double convertDurationToUnit(Double durationInDays, MeasurementUnit targetUnit) {
        if (durationInDays == null || targetUnit == null) {
            return durationInDays;
        }
        
        String unitName = targetUnit.getName().toLowerCase();
        
        switch (unitName) {
            case "day":
            case "days":
                return durationInDays;
            case "week":
            case "weeks":
                return durationInDays / 7.0;
            case "month":
            case "months":
                return durationInDays / 30.0; // Approximate
            case "year":
            case "years":
                return durationInDays / 365.0; // Approximate
            case "hour":
            case "hours":
                return durationInDays * 24.0;
            default:
                // For unknown units, return as days
                return durationInDays;
        }
    }

    /**
     * Converts duration from specified unit to days
     * @param duration Duration value
     * @param sourceUnit Source measurement unit
     * @return Duration in days
     */
    public Double convertDurationToDays(Double duration, MeasurementUnit sourceUnit) {
        if (duration == null) {
            return duration;
        }
        
        if (sourceUnit == null) {
            // Assume days if no unit specified
            return duration;
        }
        
        String unitName = sourceUnit.getName().toLowerCase();
        
        switch (unitName) {
            case "day":
            case "days":
                return duration;
            case "week":
            case "weeks":
                return duration * 7.0;
            case "month":
            case "months":
                return duration * 30.0; // Approximate
            case "year":
            case "years":
                return duration * 365.0; // Approximate
            case "hour":
            case "hours":
                return duration / 24.0;
            default:
                // For unknown units, treat as days
                return duration;
        }
    }

    /**
     * Auto-calculates prescription duration and dates based on available information
     * Updates the prescription object with calculated values
     * @param prescription The prescription to update
     */
    public void autoCalculatePrescriptionDates(Prescription prescription) {
        if (prescription == null) {
            return;
        }
        
        Date fromDate = prescription.getPrescribedFrom();
        Date toDate = prescription.getPrescribedTo();
        Double duration = prescription.getDuration();
        MeasurementUnit durationUnit = prescription.getDurationUnit();
        
        // Scenario 1: Both dates available, calculate duration
        if (fromDate != null && toDate != null) {
            Double calculatedDuration = calculateDurationFromDates(fromDate, toDate, durationUnit);
            if (calculatedDuration != null && (duration == null || !duration.equals(calculatedDuration))) {
                prescription.setDuration(calculatedDuration);
            }
        }
        // Scenario 2: From date and duration available, calculate to date
        else if (fromDate != null && duration != null && duration > 0) {
            Date calculatedToDate = calculateToDateFromDuration(fromDate, duration, durationUnit);
            if (calculatedToDate != null && (toDate == null || !toDate.equals(calculatedToDate))) {
                prescription.setPrescribedTo(calculatedToDate);
            }
        }
        // Scenario 3: To date and duration available, calculate from date
        else if (toDate != null && duration != null && duration > 0) {
            // Calculate from date by subtracting duration from to date
            Double durationInDays = convertDurationToDays(duration, durationUnit);
            if (durationInDays != null && durationInDays > 0) {
                LocalDate toLocalDate = toDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate fromLocalDate = toLocalDate.minusDays(durationInDays.longValue() - 1);
                Date calculatedFromDate = Date.from(fromLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                
                if (fromDate == null || !fromDate.equals(calculatedFromDate)) {
                    prescription.setPrescribedFrom(calculatedFromDate);
                }
            }
        }
    }

    /**
     * Validates prescription dates and duration for consistency
     * @param prescription The prescription to validate
     * @return Validation message, null if valid
     */
    public String validatePrescriptionDates(Prescription prescription) {
        if (prescription == null) {
            return "Prescription cannot be null";
        }
        
        Date fromDate = prescription.getPrescribedFrom();
        Date toDate = prescription.getPrescribedTo();
        Double duration = prescription.getDuration();
        
        // Check if to date is after from date
        if (fromDate != null && toDate != null) {
            if (toDate.before(fromDate)) {
                return "Prescribed To date cannot be before Prescribed From date";
            }
        }
        
        // Check if duration is positive
        if (duration != null && duration <= 0) {
            return "Duration must be a positive number";
        }
        
        // Check consistency between dates and duration
        if (fromDate != null && toDate != null && duration != null) {
            Double calculatedDuration = calculateDurationFromDates(fromDate, toDate, prescription.getDurationUnit());
            if (calculatedDuration != null && Math.abs(calculatedDuration - duration) > 0.1) {
                return String.format("Duration (%.1f) does not match the date range (%.1f)", duration, calculatedDuration);
            }
        }
        
        return null; // Valid
    }

    /**
     * Formats prescription period as a readable string
     * @param prescription The prescription
     * @return Formatted period string
     */
    public String formatPrescriptionPeriod(Prescription prescription) {
        if (prescription == null) {
            return "";
        }
        
        Date fromDate = prescription.getPrescribedFrom();
        Date toDate = prescription.getPrescribedTo();
        Double duration = prescription.getDuration();
        MeasurementUnit durationUnit = prescription.getDurationUnit();
        
        StringBuilder period = new StringBuilder();
        
        if (fromDate != null) {
            period.append("From: ").append(fromDate);
        }
        
        if (toDate != null) {
            if (period.length() > 0) {
                period.append(", ");
            }
            period.append("To: ").append(toDate);
        }
        
        if (duration != null) {
            if (period.length() > 0) {
                period.append(", ");
            }
            period.append("Duration: ").append(duration);
            if (durationUnit != null) {
                period.append(" ").append(durationUnit.getName());
            }
        }
        
        return period.toString();
    }
}