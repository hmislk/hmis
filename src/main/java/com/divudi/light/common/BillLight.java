package com.divudi.light.common;

import java.util.Date;
import java.util.Objects;

/**
 *
 * @author Senula Nanayakkara
 */
public class BillLight {

    private Long id;
    private String billNo;
    private Date billDate;
    private Date billTime;
    private String institutionName;
    private String departmentName;
    private String userName;
    private String patientName;
    private String patientPhone;
    private Double grossValue;
    private Double discount;
    private Double netValue;
    private Long patientId;
    private String canterName;
    private String referringDoctorName;
    
    public BillLight() {
    }

    public BillLight(Long id, String billNo, Date billDate, Date billTime, String patientName, Double netValue) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.billTime = billTime;
        this.patientName = patientName;
        this.netValue = netValue;
    }

    public BillLight(Long id, String billNo, Date billDate, String CenterName, String institutionName, String departmentName, String userName, String patientName, String patientPhone, Double grossValue, Double discount, Double netValue, Long patientId) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.canterName = CenterName;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.userName = userName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.patientId = patientId;
    }

    public BillLight(Long id, String billNo, Date billDate, String institutionName, String departmentName, String userName, String patientName, String patientPhone, Double grossValue, Double discount, Double netValue) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.userName = userName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
    }

    public BillLight(Long id, String billNo, Date billDate, String institutionName, String departmentName, String userName, String patientName, String patientPhone, Double grossValue, Double discount, Double netValue, Long patientId) {
        this.id = id;
        this.billNo = billNo;
        this.billDate = billDate;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.userName = userName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.patientId = patientId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public Double getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(Double grossValue) {
        this.grossValue = grossValue;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getReferringDoctorName() {
        return referringDoctorName;
    }

    public void setReferringDoctorName(String referringDoctorName) {
        this.referringDoctorName = referringDoctorName;
    }

    public Date getBillTime() {
        return billTime;
    }

    public void setBillTime(Date billTime) {
        this.billTime = billTime;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BillLight other = (BillLight) obj;
        return Objects.equals(this.id, other.id);
    }

    public String getCanterName() {
        return canterName;
    }

    public void setCanterName(String canterName) {
        this.canterName = canterName;
    }

}
