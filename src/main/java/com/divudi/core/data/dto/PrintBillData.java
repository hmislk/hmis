package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class PrintBillData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String departmentName;
    private String departmentPrintingName;
    private String departmentTelephone1;
    private String institutionName;
    private String institutionAddress;
    private String patientName;
    private String patientAgeSex;
    private String bhtNo;
    private String roomName;
    private String billNo;
    private Date createdAt;
    private double netTotal;

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentPrintingName() { return departmentPrintingName; }
    public void setDepartmentPrintingName(String departmentPrintingName) { this.departmentPrintingName = departmentPrintingName; }

    public String getDepartmentTelephone1() { return departmentTelephone1; }
    public void setDepartmentTelephone1(String departmentTelephone1) { this.departmentTelephone1 = departmentTelephone1; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getInstitutionAddress() { return institutionAddress; }
    public void setInstitutionAddress(String institutionAddress) { this.institutionAddress = institutionAddress; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientAgeSex() { return patientAgeSex; }
    public void setPatientAgeSex(String patientAgeSex) { this.patientAgeSex = patientAgeSex; }

    public String getBhtNo() { return bhtNo; }
    public void setBhtNo(String bhtNo) { this.bhtNo = bhtNo; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public double getNetTotal() { return netTotal; }
    public void setNetTotal(double netTotal) { this.netTotal = netTotal; }
}
