package com.divudi.core.data.dto;

import com.divudi.bean.lab.LaboratoryCommonController;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import java.io.Serializable;
import java.util.Date;

public class PatientInvestigationDTO implements Serializable {

    private Long id;
    private Long investigationId;
    private String itemCode;
    private String itemName;
    private Date billDate;
    private String billNumber;
    private String bhtNo;
    private Title patientTitle;
    private String patientName;
    private String patientAge;
    private Long patientid;
    private Sex gender;
    private Date patientDob;
    private String patientGender;
    private String patientMobile;
    private String billType;
    private PatientInvestigationStatus status;
    private Boolean billCanceled;
    private String orderedInstitution;
    private String orderedDepartment;
    private String patientNameWithTitle;
    private Boolean itemRefunded;
    private Boolean sampleAccepted;

    public PatientInvestigationDTO() {
    }

    public PatientInvestigationDTO(Long id, String itemName, Date billDate, String billNumber, Title patientTitle, String patientName, Long patientid, Sex gender) {
        this.id = id;
        this.itemName = itemName;
        this.billDate = billDate;
        this.billNumber = billNumber;
        this.patientTitle = patientTitle;
        this.patientName = patientName;
        this.patientid = patientid;
        this.gender = gender;
    }

    // For use Laboratory Dashboard
    public PatientInvestigationDTO(
            Long investigationId,
            String itemName,
            String billNumber,
            Date billDate,
            Long patientId,
            Title patientTitle,
            String patientName,
            Date patientDob,
            String patientGender,
            String patientMobile,
            String billType,
            PatientInvestigationStatus status,
            Boolean billCanceled,
            Boolean itemRefunded,
            Boolean sampleAccepted,
            String orderedInstitution,
            String orderedDepartment
            
    ) {
        this.investigationId = investigationId;
        this.itemName = itemName;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.patientTitle = patientTitle;
        this.patientName = patientName;
        this.patientDob = patientDob;
        this.patientGender = patientGender;
        this.patientMobile = patientMobile;
        this.billType = billType;
        this.status = status;
        this.billCanceled = billCanceled;
        this.itemRefunded = itemRefunded;
        this.sampleAccepted = sampleAccepted;
        this.orderedInstitution = orderedInstitution;
        this.orderedDepartment = orderedDepartment;
        

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getPatientid() {
        return patientid;
    }

    public void setPatientid(Long patientid) {
        this.patientid = patientid;
    }

    public Sex getGender() {
        return gender;
    }

    public void setGender(Sex gender) {
        this.gender = gender;
    }

    public Title getPatientTitle() {
        return patientTitle;
    }

    public void setPatientTitle(Title patientTitle) {
        this.patientTitle = patientTitle;
    }

    public String getPatientAge() {
        patientAge = LaboratoryCommonController.calculateAge(patientDob,billDate);
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }
    
    public String getPatientNameWithTitle() {
        String temT;
        Title t = getPatientTitle();
        if (t != null) {
            temT = t.getLabel();
        } else {
            temT = "";
        }
        patientNameWithTitle = temT + " " + getPatientName();
        return patientNameWithTitle;
    }

    public void setPatientNameWithTitle(String patientNameWithTitle) {
        this.patientNameWithTitle = patientNameWithTitle;
    }

    public Date getPatientDob() {
        return patientDob;
    }

    public void setPatientDob(Date patientDob) {
        this.patientDob = patientDob;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getPatientMobile() {
        return patientMobile;
    }

    public void setPatientMobile(String patientMobile) {
        this.patientMobile = patientMobile;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public PatientInvestigationStatus getStatus() {
        return status;
    }

    public void setStatus(PatientInvestigationStatus status) {
        this.status = status;
    }

    public Boolean getBillCanceled() {
        return billCanceled;
    }

    public void setBillCanceled(Boolean billCanceled) {
        this.billCanceled = billCanceled;
    }

    public String getOrderedInstitution() {
        return orderedInstitution;
    }

    public void setOrderedInstitution(String orderedInstitution) {
        this.orderedInstitution = orderedInstitution;
    }

    public String getOrderedDepartment() {
        return orderedDepartment;
    }

    public void setOrderedDepartment(String orderedDepartment) {
        this.orderedDepartment = orderedDepartment;
    }

    public Long getInvestigationId() {
        return investigationId;
    }

    public void setInvestigationId(Long investigationId) {
        this.investigationId = investigationId;
    }

    public Boolean getItemRefunded() {
        return itemRefunded;
    }

    public void setItemRefunded(Boolean itemRefunded) {
        this.itemRefunded = itemRefunded;
    }

    public Boolean getSampleAccepted() {
        return sampleAccepted;
    }

    public void setSampleAccepted(Boolean sampleAccepted) {
        this.sampleAccepted = sampleAccepted;
    }

}
