package com.divudi.core.data.dto;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * DTO for OPD Bill Item list report.
 * All fields are flat — no lazy entity traversal needed in XHTML.
 */
public class OpdBillItemDTO implements Serializable {

    // BillItem identity
    private Long billItemId;

    // Bill fields
    private Long billId;
    private String deptId;
    private BillTypeAtomic billTypeAtomic;
    private BillClassType billClassType;
    private Date billedAt;
    private Boolean cancelled;
    private Boolean refunded;
    private PaymentMethod paymentMethod;
    private String paymentSchemeName;

    // Institution / department
    private String toInstitutionName;
    private String creditCompanyName;

    // Patient
    private String patientName;
    private Object patientTitle;   // Title enum — display via toString()
    private Date patientDob;       // Used to calculate age
    private Object patientSex;     // Sex enum — display via toString()
    private String patientPhone;

    // Item / category
    private String categoryName;
    private String itemName;

    // Billed by
    private String billedByName;

    // Cancellation info (from cancelledBill via LEFT JOIN)
    private Date cancelledAt;
    private String cancelledByName;

    // Refund info (from refundedBill via LEFT JOIN)
    private Date refundedAt;
    private String refundedByName;

    // Financial
    private Double grossValue;
    private Double discount;
    private Double netValue;

    // Doctor
    private String doctorName;
    private Object doctorTitle;    // Title enum — display via toString()

    // Navigation IDs
    private Long backwardReferenceBillId;

    public OpdBillItemDTO() {
    }

    public OpdBillItemDTO(
            Long billItemId,
            Long billId,
            String deptId,
            BillTypeAtomic billTypeAtomic,
            BillClassType billClassType,
            Date billedAt,
            Boolean cancelled,
            Boolean refunded,
            PaymentMethod paymentMethod,
            String paymentSchemeName,
            String toInstitutionName,
            String creditCompanyName,
            String patientName,
            Object patientTitle,
            Date patientDob,
            Object patientSex,
            String patientPhone,
            String categoryName,
            String itemName,
            String billedByName,
            Date cancelledAt,
            String cancelledByName,
            Date refundedAt,
            String refundedByName,
            Double grossValue,
            Double discount,
            Double netValue,
            String doctorName,
            Object doctorTitle,
            Long backwardReferenceBillId) {
        this.billItemId = billItemId;
        this.billId = billId;
        this.deptId = deptId;
        this.billTypeAtomic = billTypeAtomic;
        this.billClassType = billClassType;
        this.billedAt = billedAt;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.paymentMethod = paymentMethod;
        this.paymentSchemeName = paymentSchemeName;
        this.toInstitutionName = toInstitutionName;
        this.creditCompanyName = creditCompanyName;
        this.patientName = patientName;
        this.patientTitle = patientTitle;
        this.patientDob = patientDob;
        this.patientSex = patientSex;
        this.patientPhone = patientPhone;
        this.categoryName = categoryName;
        this.itemName = itemName;
        this.billedByName = billedByName;
        this.cancelledAt = cancelledAt;
        this.cancelledByName = cancelledByName;
        this.refundedAt = refundedAt;
        this.refundedByName = refundedByName;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.doctorName = doctorName;
        this.doctorTitle = doctorTitle;
        this.backwardReferenceBillId = backwardReferenceBillId;
    }

    // Convenience display methods (non-JPQL, safe in XHTML)
    public String getPatientNameWithTitle() {
        String titleStr = patientTitle != null ? patientTitle.toString() : "";
        String nameStr = patientName != null ? patientName : "";
        if (!titleStr.isEmpty()) {
            return titleStr + " " + nameStr;
        }
        return nameStr;
    }

    public String getDoctorNameWithTitle() {
        String titleStr = doctorTitle != null ? doctorTitle.toString() : "";
        String nameStr = doctorName != null ? doctorName : "";
        if (!titleStr.isEmpty()) {
            return titleStr + " " + nameStr;
        }
        return nameStr;
    }

    public String getPatientAgeAsString() {
        if (patientDob == null) {
            return "";
        }
        Calendar dob = Calendar.getInstance();
        dob.setTime(patientDob);
        Calendar now = Calendar.getInstance();
        int years = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            years--;
        }
        return years + " Y";
    }

    // Getters and setters

    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public BillTypeAtomic getBillTypeAtomic() { return billTypeAtomic; }
    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) { this.billTypeAtomic = billTypeAtomic; }

    public BillClassType getBillClassType() { return billClassType; }
    public void setBillClassType(BillClassType billClassType) { this.billClassType = billClassType; }

    public Date getBilledAt() { return billedAt; }
    public void setBilledAt(Date billedAt) { this.billedAt = billedAt; }

    public Boolean getCancelled() { return cancelled != null && cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public Boolean getRefunded() { return refunded != null && refunded; }
    public void setRefunded(Boolean refunded) { this.refunded = refunded; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentSchemeName() { return paymentSchemeName; }
    public void setPaymentSchemeName(String paymentSchemeName) { this.paymentSchemeName = paymentSchemeName; }

    public String getToInstitutionName() { return toInstitutionName; }
    public void setToInstitutionName(String toInstitutionName) { this.toInstitutionName = toInstitutionName; }

    public String getCreditCompanyName() { return creditCompanyName; }
    public void setCreditCompanyName(String creditCompanyName) { this.creditCompanyName = creditCompanyName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Object getPatientTitle() { return patientTitle; }
    public void setPatientTitle(Object patientTitle) { this.patientTitle = patientTitle; }

    public Date getPatientDob() { return patientDob; }
    public void setPatientDob(Date patientDob) { this.patientDob = patientDob; }

    public Object getPatientSex() { return patientSex; }
    public void setPatientSex(Object patientSex) { this.patientSex = patientSex; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getBilledByName() { return billedByName; }
    public void setBilledByName(String billedByName) { this.billedByName = billedByName; }

    public Date getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Date cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelledByName() { return cancelledByName; }
    public void setCancelledByName(String cancelledByName) { this.cancelledByName = cancelledByName; }

    public Date getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Date refundedAt) { this.refundedAt = refundedAt; }

    public String getRefundedByName() { return refundedByName; }
    public void setRefundedByName(String refundedByName) { this.refundedByName = refundedByName; }

    public Double getGrossValue() { return grossValue != null ? grossValue : 0.0; }
    public void setGrossValue(Double grossValue) { this.grossValue = grossValue; }

    public Double getDiscount() { return discount != null ? discount : 0.0; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public Double getNetValue() { return netValue != null ? netValue : 0.0; }
    public void setNetValue(Double netValue) { this.netValue = netValue; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public Object getDoctorTitle() { return doctorTitle; }
    public void setDoctorTitle(Object doctorTitle) { this.doctorTitle = doctorTitle; }

    public Long getBackwardReferenceBillId() { return backwardReferenceBillId; }
    public void setBackwardReferenceBillId(Long backwardReferenceBillId) { this.backwardReferenceBillId = backwardReferenceBillId; }
}
