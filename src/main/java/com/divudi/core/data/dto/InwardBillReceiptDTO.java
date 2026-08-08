package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.Date;

/**
 * Print DTO shared by the Appointment Deposit Conversion receipts (Issue
 * #22783, Part A): the INWARD_APPOINTMENT_CANCEL_BILL receipt and the
 * INWARD_DEPOSIT receipt created by
 * {@code AdmissionController.convertAppointmentDepositToInwardDeposit()}.
 *
 * All fields are populated from persisted database columns only, via the
 * JPQL constructor below (see {@code BillFacade.findInwardBillReceiptDTO}).
 * Derived/computed values (name-with-title, age) are added as plain Java
 * getters after construction since JPQL cannot call derived getters.
 */
public class InwardBillReceiptDTO implements Serializable {

    private Long billId;
    private String deptId;
    private Date billDate;
    private PaymentMethod paymentMethod;
    private Double amount;
    private String comments;
    private String referenceBillDeptId;
    private String departmentPrintingName;
    private String departmentAddress;
    private String departmentTelephone1;
    private String departmentTelephone2;
    private String departmentFax;
    private String departmentEmail;
    private Title patientTitle;
    private String patientName;
    private Date patientDob;
    private Sex patientSex;
    private String admissionTypeName;
    private String bhtNo;
    private Title cashierTitle;
    private String cashierName;

    public InwardBillReceiptDTO() {
    }

    public InwardBillReceiptDTO(Long billId,
            String deptId,
            Date billDate,
            PaymentMethod paymentMethod,
            Double amount,
            String comments,
            String referenceBillDeptId,
            String departmentPrintingName,
            String departmentAddress,
            String departmentTelephone1,
            String departmentTelephone2,
            String departmentFax,
            String departmentEmail,
            Title patientTitle,
            String patientName,
            Date patientDob,
            Sex patientSex,
            String admissionTypeName,
            String bhtNo,
            Title cashierTitle,
            String cashierName) {
        this.billId = billId;
        this.deptId = deptId;
        this.billDate = billDate;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.comments = comments;
        this.referenceBillDeptId = referenceBillDeptId;
        this.departmentPrintingName = departmentPrintingName;
        this.departmentAddress = departmentAddress;
        this.departmentTelephone1 = departmentTelephone1;
        this.departmentTelephone2 = departmentTelephone2;
        this.departmentFax = departmentFax;
        this.departmentEmail = departmentEmail;
        this.patientTitle = patientTitle;
        this.patientName = patientName;
        this.patientDob = patientDob;
        this.patientSex = patientSex;
        this.admissionTypeName = admissionTypeName;
        this.bhtNo = bhtNo;
        this.cashierTitle = cashierTitle;
        this.cashierName = cashierName;
    }

    public Long getBillId() {
        return billId;
    }

    public String getDeptId() {
        return deptId;
    }

    public Date getBillDate() {
        return billDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public String getComments() {
        return comments;
    }

    public String getReferenceBillDeptId() {
        return referenceBillDeptId;
    }

    public String getDepartmentPrintingName() {
        return departmentPrintingName;
    }

    public String getDepartmentAddress() {
        return departmentAddress;
    }

    public String getDepartmentTelephone1() {
        return departmentTelephone1;
    }

    public String getDepartmentTelephone2() {
        return departmentTelephone2;
    }

    public String getDepartmentFax() {
        return departmentFax;
    }

    public String getDepartmentEmail() {
        return departmentEmail;
    }

    public Title getPatientTitle() {
        return patientTitle;
    }

    public String getPatientName() {
        return patientName;
    }

    public Date getPatientDob() {
        return patientDob;
    }

    public Sex getPatientSex() {
        return patientSex;
    }

    public String getAdmissionTypeName() {
        return admissionTypeName;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public Title getCashierTitle() {
        return cashierTitle;
    }

    public String getCashierName() {
        return cashierName;
    }

    /**
     * Mirrors {@code Person.getNameWithTitle()}'s composition
     * (title label + " " + name) since JPQL cannot call that derived getter.
     */
    public String getPatientNameWithTitle() {
        String temT;
        if (patientTitle != null) {
            temT = patientTitle.getLabel();
        } else {
            temT = "";
        }
        return temT + " " + (patientName != null ? patientName : "");
    }

    /**
     * Mirrors {@code Person.getNameWithTitle()}'s composition for the
     * cashier (creater.webUserPerson) title + name.
     */
    public String getCashierNameWithTitle() {
        String temT;
        if (cashierTitle != null) {
            temT = cashierTitle.getLabel();
        } else {
            temT = "";
        }
        return temT + " " + (cashierName != null ? cashierName : "");
    }

    /**
     * Mirrors {@code Person.getAgeAsString()}'s "Not Recorded" fallback,
     * computed via {@link CommonFunctions#calculateAge(Date, Date)} against
     * the bill date (JPQL cannot call the derived age getter directly).
     */
    public String getPatientAgeAsString() {
        if (patientDob == null) {
            return "Not Recorded";
        }
        String age = new CommonFunctions().calculateAge(patientDob, billDate);
        if (age == null || age.trim().isEmpty()) {
            return "Not Recorded";
        }
        return age;
    }
}
