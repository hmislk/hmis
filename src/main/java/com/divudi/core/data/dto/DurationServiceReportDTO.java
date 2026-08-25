package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

public class DurationServiceReportDTO implements Serializable {

    private Long patientItemId;
    private String bhtNo;
    private String mrnNo;
    private Title title;
    private String consultantName;
    private String surgeryName;
    private String serviceDepartmentName;
    private String serviceName;
    private String serviceGroupName;
    private Date startTime;
    private Date endTime;
    private Double basePrice;
    private Double discountAmount;
    private Double sponsorDiscount;
    private Double sponsorNet;
    private Double patientAmount;
    private Double adjustedAmount;
    private String creatorName;
    private String checkedByName;
    private Date checkedAt;
    private Date serviceAddedAt;
    private Date invoiceDate;
    private String creatingLocation;

    public DurationServiceReportDTO() {
    }

    public DurationServiceReportDTO(
            Long patientItemId,
            String bhtNo,
            String mrnNo,
            String consultantName,
            String surgeryName,
            String serviceDepartmentName,
            String serviceName,
            String serviceGroupName,
            Date startTime,
            Date endTime,
            Double basePrice,
            Double discountAmount,
            Double adjustedAmount,
            String creatorName,
            String checkedByName,
            Date checkedAt,
            Date serviceAddedAt,
            Date invoiceDate) {
        this.patientItemId = patientItemId;
        this.bhtNo = bhtNo;
        this.mrnNo = mrnNo;
        this.consultantName = consultantName;
        this.surgeryName = surgeryName;
        this.serviceDepartmentName = serviceDepartmentName;
        this.serviceName = serviceName;
        this.serviceGroupName = serviceGroupName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice != null ? basePrice : 0.0;
        this.discountAmount = discountAmount != null ? discountAmount : 0.0;
        this.sponsorDiscount = 0.0;
        this.sponsorNet = 0.0;
        recalculatePatientAmount();
        this.adjustedAmount = adjustedAmount != null ? adjustedAmount : 0.0;
        this.creatorName = creatorName;
        this.checkedByName = checkedByName;
        this.checkedAt = checkedAt;
        this.serviceAddedAt = serviceAddedAt;
        this.invoiceDate = invoiceDate;
    }

    public DurationServiceReportDTO(
            Long patientItemId,
            String bhtNo,
            String mrnNo,
            Title title,
            String consultantNameWithInitials,
            String surgeryNameFromProc,
            String surgeryNameFromEncounter,
            String serviceDepartmentName,
            String serviceName,
            String serviceGroupName,
            Date startTime,
            Date endTime,
            Double basePrice,
            Double discountAmount,
            Double adjustedAmount,
            String creatorNameWithInitials,
            String billCheckedByWithInitials,
            String finalBillCheckedByWithInitials,
            Date billCheckedAt,
            Date finalBillCheckedAt,
            Date serviceAddedAt,
            Date billCreatedAt,
            Date finalBillCreatedAt,
            Long creditCompanyId,
            String creatingLocation) {
        this.patientItemId = patientItemId;
        this.bhtNo = bhtNo != null ? bhtNo : "";
        this.mrnNo = mrnNo != null ? mrnNo : "";
        this.title = title;
        this.consultantName = consultantNameWithInitials != null ? consultantNameWithInitials : "";
        this.creatingLocation = creatingLocation != null ? creatingLocation : "";
        
        if (surgeryNameFromProc != null && !surgeryNameFromProc.trim().isEmpty()) {
            this.surgeryName = surgeryNameFromProc;
        } else if (surgeryNameFromEncounter != null && !surgeryNameFromEncounter.trim().isEmpty()) {
            this.surgeryName = surgeryNameFromEncounter;
        } else {
            this.surgeryName = "";
        }
        
        this.serviceDepartmentName = serviceDepartmentName != null ? serviceDepartmentName : "";
        this.serviceName = serviceName != null ? serviceName : "";
        this.serviceGroupName = serviceGroupName != null ? serviceGroupName : "";
        this.startTime = startTime;
        this.endTime = endTime;
        
        double bp = basePrice != null ? basePrice : 0.0;
        double da = discountAmount != null ? discountAmount : 0.0;
        
        this.basePrice = bp;
        this.discountAmount = da;
        
        double net = bp - da;
        if (creditCompanyId != null) {
            this.sponsorNet = net;
            this.sponsorDiscount = 0.0;
            this.patientAmount = 0.0;
        } else {
            this.sponsorNet = 0.0;
            this.sponsorDiscount = 0.0;
            this.patientAmount = net;
        }
        
        this.adjustedAmount = adjustedAmount != null ? adjustedAmount : 0.0;
        this.creatorName = creatorNameWithInitials != null ? creatorNameWithInitials : "";
        this.checkedByName = billCheckedByWithInitials != null ? billCheckedByWithInitials : (finalBillCheckedByWithInitials != null ? finalBillCheckedByWithInitials : "");
        this.checkedAt = billCheckedAt != null ? billCheckedAt : finalBillCheckedAt;
        this.serviceAddedAt = serviceAddedAt;
        this.invoiceDate = billCreatedAt != null ? billCreatedAt : finalBillCreatedAt;
    }

    public String getDuration() {
        if (startTime == null || endTime == null) {
            return "";
        }
        long minutes = Math.max(0, (endTime.getTime() - startTime.getTime()) / (1000 * 60));
        long days = minutes / (24 * 60);
        long hours = (minutes % (24 * 60)) / 60;
        long mins = minutes % 60;
        return days + "D " + hours + "H " + mins + "M";
    }

    public Long getPatientItemId() {
        return patientItemId;
    }

    public void setPatientItemId(Long patientItemId) {
        this.patientItemId = patientItemId;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getMrnNo() {
        return mrnNo;
    }

    public void setMrnNo(String mrnNo) {
        this.mrnNo = mrnNo;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public String getSurgeryName() {
        return surgeryName;
    }

    public void setSurgeryName(String surgeryName) {
        this.surgeryName = surgeryName;
    }

    public String getServiceDepartmentName() {
        return serviceDepartmentName;
    }

    public void setServiceDepartmentName(String serviceDepartmentName) {
        this.serviceDepartmentName = serviceDepartmentName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceGroupName() {
        return serviceGroupName;
    }

    public void setServiceGroupName(String serviceGroupName) {
        this.serviceGroupName = serviceGroupName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Double basePrice) {
        this.basePrice = basePrice;
        recalculatePatientAmount();
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
        recalculatePatientAmount();
    }

    public Double getSponsorDiscount() {
        return sponsorDiscount;
    }

    public void setSponsorDiscount(Double sponsorDiscount) {
        this.sponsorDiscount = sponsorDiscount;
        recalculatePatientAmount();
    }

    public Double getSponsorNet() {
        return sponsorNet;
    }

    public void setSponsorNet(Double sponsorNet) {
        this.sponsorNet = sponsorNet;
    }

    public Double getPatientAmount() {
        return patientAmount;
    }

    public void setPatientAmount(Double patientAmount) {
        this.patientAmount = patientAmount;
    }

    private void recalculatePatientAmount() {
        double bp = basePrice != null ? basePrice : 0.0;
        double da = discountAmount != null ? discountAmount : 0.0;
        double sd = sponsorDiscount != null ? sponsorDiscount : 0.0;
        this.patientAmount = bp - da - sd;
    }

    public Double getAdjustedAmount() {
        return adjustedAmount;
    }

    public void setAdjustedAmount(Double adjustedAmount) {
        this.adjustedAmount = adjustedAmount;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCheckedByName() {
        return checkedByName;
    }

    public void setCheckedByName(String checkedByName) {
        this.checkedByName = checkedByName;
    }

    public Date getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Date checkedAt) {
        this.checkedAt = checkedAt;
    }

    public Date getServiceAddedAt() {
        return serviceAddedAt;
    }

    public void setServiceAddedAt(Date serviceAddedAt) {
        this.serviceAddedAt = serviceAddedAt;
    }

    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public Title getTitle() {
        return title;
    }

    public void setTitle(Title title) {
        this.title = title;
    }

    public String getCreatingLocation() {
        return creatingLocation;
    }

    public void setCreatingLocation(String creatingLocation) {
        this.creatingLocation = creatingLocation;
    }
}
