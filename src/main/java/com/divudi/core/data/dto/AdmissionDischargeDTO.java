package com.divudi.core.data.dto;

import com.divudi.core.data.Sex;
import java.io.Serializable;
import java.util.Date;
import java.time.Duration;

public class AdmissionDischargeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Patient / Identification
    private String phn;
    private String patientName;
    private String mobileNumber;
    private String bhtNo;

    // Raw address - parsed in constructor
    private String rawAddress;
    private String houseNo;
    private String streetVillage;
    private String locality;
    private String district;
    private String city;
    private String state;
    private String country;
    private String pin;

    // Admission details
    private String comments;
    private String admissionTypeName;
    private Integer age;
    private Sex sex;
    private String departmentName;
    private Date dateOfAdmission;
    private Date dateOfDischarge;
    private Date dateOfBirth;

    // Discharge details
    private String dischargeTypeName;
    private String wardBedName;
    private String consultantName;
    private String sponsorName;

    // Financials
    private double totalCompanyPaidAtFinalProcessing;
    private double totalPatientPaidAtFinalProcessing;
    private double discount;
    private double netTotal;
    private double amountDueAtFinalProcessing;

    // Users / Bill
    private String clinicalDischargedByName;
    private Date clinicalDischargeDateTime;
    private String finalBillCreatorName;
    private Date finalBillCreatedAt;

    // Calculated fields
    private Double losDays;
    private String tatFormatted;
    private String billingTatFormatted;

    /**
     * Constructor used by JPQL "SELECT new
     * com.example.dto.AdmissionDischargeDTO(...)". Order of arguments MUST
     * exactly match the JPQL constructor expression.
     */
    public AdmissionDischargeDTO(
            String phn,
            String patientName,
            String bhtNo,
            String rawAddress,
            String comments,
            String admissionTypeName,
            Date dateOfBirth,
            Sex sex,
            String departmentName,
            Date dateOfAdmission,
            Date dateOfDischarge,
            String dischargeTypeName,
            String wardBedName,
            String consultantName,
            String sponsorName,
            double totalCompanyPaidAtFinalProcessing,
            double totalPatientPaidAtFinalProcessing,
            double discount,
            double netTotal,
            double amountDueAtFinalProcessing,
            String clinicalDischargedByName,
            Date clinicalDischargeDateTime,
            String finalBillCreatorName,
            Date finalBillCreatedAt
    ) {
        this.phn = phn;
        this.patientName = patientName;
        this.bhtNo = bhtNo;
        this.rawAddress = rawAddress;
        this.comments = comments;
        this.admissionTypeName = admissionTypeName;
        this.dateOfBirth = dateOfBirth;
        this.age = calculateAge(dateOfBirth);
        this.sex = sex;
        this.departmentName = departmentName;
        this.dateOfAdmission = dateOfAdmission;
        this.dateOfDischarge = dateOfDischarge;
        this.dischargeTypeName = dischargeTypeName;
        this.wardBedName = wardBedName;
        this.consultantName = consultantName;
        this.sponsorName = sponsorName;
        this.totalCompanyPaidAtFinalProcessing = totalCompanyPaidAtFinalProcessing;
        this.totalPatientPaidAtFinalProcessing = totalPatientPaidAtFinalProcessing;
        this.discount = discount;
        this.netTotal = netTotal;
        this.amountDueAtFinalProcessing = amountDueAtFinalProcessing;
        this.clinicalDischargedByName = clinicalDischargedByName;
        this.clinicalDischargeDateTime = clinicalDischargeDateTime;
        this.finalBillCreatorName = finalBillCreatorName;
        this.finalBillCreatedAt = finalBillCreatedAt;

        parseAddress(rawAddress);
        calculateLos();
        calculateTat();
        calculateBillingTat();
    }

    private Integer calculateAge(Date dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        java.time.LocalDate birthDate = dateOfBirth.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        java.time.LocalDate today = java.time.LocalDate.now();
        return java.time.Period.between(birthDate, today).getYears();
    }

    /**
     * Parses a comma-separated address string into components. Expected format
     * (most-specific to least-specific): "House No, Street/Village, Locality,
     * District, City, State, Country, PIN" Missing parts are tolerated - any
     * unmatched parts are left blank.
     */
    private void parseAddress(String raw) {
        this.houseNo = "";
        this.streetVillage = "";
        this.locality = "";
        this.district = "";
        this.city = "";
        this.state = "";
        this.country = "";
        this.pin = "";

        if (raw == null || raw.trim().isEmpty()) {
            return;
        }

        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        // Map positionally - tolerant of fewer parts
        if (parts.length > 0) {
            this.houseNo = parts[0];
        }
        if (parts.length > 1) {
            this.streetVillage = parts[1];
        }
        if (parts.length > 2) {
            this.locality = parts[2];
        }
        if (parts.length > 3) {
            this.district = parts[3];
        }
        if (parts.length > 4) {
            this.city = parts[4];
        }
        if (parts.length > 5) {
            this.state = parts[5];
        }
        if (parts.length > 6) {
            this.country = parts[6];
        }
        if (parts.length > 7) {
            this.pin = parts[7];
        }
    }

    /**
     * LOS = total elapsed time between admission and clinical discharge,
     * expressed as a decimal number of days (e.g. 1.5).
     */
    private void calculateLos() {
        if (dateOfAdmission == null || clinicalDischargeDateTime == null) {
            this.losDays = null;
            return;
        }
        long millis = clinicalDischargeDateTime.getTime() - dateOfAdmission.getTime();
        if (millis < 0) {
            this.losDays = null;
            return;
        }
        double days = millis / (1000.0 * 60.0 * 60.0 * 24.0);
        // Round to 2 decimal places
        this.losDays = Math.round(days * 100.0) / 100.0;
    }

    /**
     * TAT = same duration as LOS, formatted as total HH:mm:ss (hours are NOT
     * capped at 24 - e.g. 1.5 days = 36:00:00).
     */
    private void calculateTat() {
        if (dateOfAdmission == null || clinicalDischargeDateTime == null) {
            this.tatFormatted = "";
            return;
        }
        Duration duration = Duration.between(dateOfAdmission.toInstant(), clinicalDischargeDateTime.toInstant());
        this.tatFormatted = formatDuration(duration);
    }

    /**
     * Billing TAT = elapsed time between clinical discharge and final bill
     * creation, formatted as HH:mm:ss.
     */
    private void calculateBillingTat() {
        if (clinicalDischargeDateTime == null || finalBillCreatedAt == null) {
            this.billingTatFormatted = "";
            return;
        }
        Duration duration = Duration.between(clinicalDischargeDateTime.toInstant(), finalBillCreatedAt.toInstant());
        this.billingTatFormatted = formatDuration(duration);
    }

    /**
     * Formats a Duration as total HH:mm:ss (hours can exceed 24). Negative
     * durations are formatted with a leading "-".
     */
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "";
        }
        boolean negative = duration.isNegative();
        Duration abs = duration.abs();

        long totalSeconds = abs.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return negative ? "-" + formatted : formatted;
    }

    // ===================== Getters =====================
    public String getPhn() {
        return phn;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public String getRawAddress() {
        return rawAddress;
    }

    public String getHouseNo() {
        return houseNo;
    }

    public String getStreetVillage() {
        return streetVillage;
    }

    public String getLocality() {
        return locality;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public String getPin() {
        return pin;
    }

    public String getComments() {
        return comments;
    }

    public String getAdmissionTypeName() {
        return admissionTypeName;
    }

    public Integer getAge() {
        return age;
    }

    public Sex getSex() {
        return sex;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public Date getDateOfAdmission() {
        return dateOfAdmission;
    }

    public Date getDateOfDischarge() {
        return dateOfDischarge;
    }

    public String getDischargeTypeName() {
        return dischargeTypeName;
    }

    public String getWardBedName() {
        return wardBedName;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public String getSponsorName() {
        return sponsorName;
    }

    public double getTotalCompanyPaidAtFinalProcessing() {
        return totalCompanyPaidAtFinalProcessing;
    }

    public double getTotalPatientPaidAtFinalProcessing() {
        return totalPatientPaidAtFinalProcessing;
    }

    public double getDiscount() {
        return discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public double getAmountDueAtFinalProcessing() {
        return amountDueAtFinalProcessing;
    }

    public String getClinicalDischargedByName() {
        return clinicalDischargedByName;
    }

    public Date getClinicalDischargeDateTime() {
        return clinicalDischargeDateTime;
    }

    public String getFinalBillCreatorName() {
        return finalBillCreatorName;
    }

    public Date getFinalBillCreatedAt() {
        return finalBillCreatedAt;
    }

    public Double getLosDays() {
        return losDays;
    }

    public String getTatFormatted() {
        return tatFormatted;
    }

    public String getBillingTatFormatted() {
        return billingTatFormatted;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }
}
