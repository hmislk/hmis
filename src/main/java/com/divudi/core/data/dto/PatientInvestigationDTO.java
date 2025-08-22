package com.divudi.core.data.dto;

import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

public class PatientInvestigationDTO implements Serializable {

    private Long id;
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

}
