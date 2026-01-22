package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.PatientRoom;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class IpUnsettledInvoiceDTO implements Serializable {

    private Long admissionId;
    private String phn;
    private String patientName;
    private Title patientTitle;
    private String mobileNumber;
    private Date dateOfBirth;
    private PatientRoom patientRoom;
    private Date dateOfDischarge;
    private Boolean paymentFinalized;
    private Double netTotal;
    private Double creditPaidAmount;
    private WebUser creater;

    // Updated constructor with primitive types for potential JPA matching
    public IpUnsettledInvoiceDTO(
            Long admissionId,
            String phn,
            String patientName,
            Title patientTitle,
            String mobileNumber,
            Date dateOfBirth,
            PatientRoom patientRoom,
            Date dateOfDischarge,
            boolean paymentFinalized, // Primitive boolean
            double netTotal, // Primitive double
            double creditPaidAmount, // Primitive double
            WebUser creater) {
        this.admissionId = admissionId;
        this.phn = phn;
        this.patientName = patientName;
        this.patientTitle = patientTitle;
        this.mobileNumber = mobileNumber;
        this.dateOfBirth = dateOfBirth;
        this.patientRoom = patientRoom;
        this.dateOfDischarge = dateOfDischarge;
        this.paymentFinalized = paymentFinalized; // Auto-box to Boolean
        this.netTotal = netTotal;                 // Auto-box to Double
        this.creditPaidAmount = creditPaidAmount; // Auto-box to Double
        this.creater = creater;
    }

    // Computed properties
    public String getPatientNameWithTitle() {
        if (patientTitle != null && patientName != null) {
            return patientTitle.getLabel() + " " + patientName;
        }
        return patientName != null ? patientName : "";
    }

    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }

        Calendar dob = Calendar.getInstance();
        dob.setTime(dateOfBirth);
        Calendar now = Calendar.getInstance();

        int age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        // Adjust if birthday hasn't occurred this year
        if (now.get(Calendar.MONTH) < dob.get(Calendar.MONTH)
                || (now.get(Calendar.MONTH) == dob.get(Calendar.MONTH)
                && now.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        return age;
    }

    public Double getAmountToBePaid() {
        double total = netTotal != null ? netTotal : 0.0;
        double collected = creditPaidAmount != null ? creditPaidAmount : 0.0;
        return total - collected;
    }

    public String getPaymentStatusLabel() {
        return Boolean.TRUE.equals(paymentFinalized) ? "Finalized" : "Pending";
    }

    // Getters and Setters (unchanged)
    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public String getPhn() {
        return phn;
    }

    public void setPhn(String phn) {
        this.phn = phn;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Title getPatientTitle() {
        return patientTitle;
    }

    public void setPatientTitle(Title patientTitle) {
        this.patientTitle = patientTitle;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public PatientRoom getRoomCategoryName() {
        return patientRoom;
    }

    public void setRoomCategoryName(PatientRoom patientRoom) {
        this.patientRoom = patientRoom;
    }

    public Date getDateOfDischarge() {
        return dateOfDischarge;
    }

    public void setDateOfDischarge(Date dateOfDischarge) {
        this.dateOfDischarge = dateOfDischarge;
    }

    public Boolean getPaymentFinalized() {
        return paymentFinalized;
    }

    public void setPaymentFinalized(Boolean paymentFinalized) {
        this.paymentFinalized = paymentFinalized;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getCreditPaidAmount() {
        return creditPaidAmount;
    }

    public void setCreditPaidAmount(Double creditPaidAmount) {
        this.creditPaidAmount = creditPaidAmount;
    }

    public WebUser getCreaterName() {
        return creater;
    }

    public void setCreaterName(WebUser creater) {
        this.creater = creater;
    }
}
