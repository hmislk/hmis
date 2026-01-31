package com.divudi.core.data.dto;

import com.divudi.bean.lab.LaboratoryCommonController;
import com.divudi.core.data.Title;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import java.io.Serializable;
import java.util.Date;
import javax.inject.Inject;

public class SampleDTO implements Serializable {

    private Long id;
    private Long sampleId;
    private Date sampleDate;
    private String billNumber;
    
    private String analyzerName;
    private String billType;
    private String sampleTube;
    private Date billDate;
    private Long patientId;
    private String bhtNo;
    private PatientInvestigationStatus status;
    private Boolean billCanceled;
    
    private String collectingCenterName;
    
    private String patientNameWithTitle;
    private Title patientTitle;
    private String patientName;
    private Date patientDob;
    private String patientGender;
    private String patientMobile;
    private String patientAge;
    
    private Date createdAt;

    private Boolean barcodeGenerated;
    private Date barcodeGeneratedAt;

    private Boolean sampleCollected;
    private Date sampleCollectedAt;

    private Boolean sampleSent;
    private Date sampleSentAt;

    private Boolean sampleReceivedAtLab;
    private Date sampleReceivedAtLabAt;

    private Boolean cancelled;
    private Date cancelledAt;

    private Boolean sampleRejected;
    private Date sampleRejectedAt;
    
    public SampleDTO() {
    }

    public SampleDTO(Long id, Long sampleId, Date sampleDate, String billNumber, String patientName, String analyzerName) {
        this.id = id;
        this.sampleId = sampleId;
        this.sampleDate = sampleDate;
        this.billNumber = billNumber;
        this.patientName = patientName;
        this.analyzerName = analyzerName;
    }
    
    // For use Laboratory Dashboard
    public SampleDTO(
            Long sampleId, 
            String sampleTube,
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
            Date createdAt,
            Boolean barcodeGenerated, 
            Date barcodeGeneratedAt, 
            Boolean sampleCollected, 
            Date sampleCollectedAt,
            Boolean sampleSent, 
            Date sampleSentAt, 
            Boolean sampleReceivedAtLab, 
            Date sampleReceivedAtLabAt, 
            Boolean cancelled, 
            Date cancelledAt, 
            Boolean sampleRejected, 
            Date sampleRejectedAt
            
    ) {
        this.sampleId = sampleId;
        this.sampleTube = sampleTube;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.patientId = patientId;
        this.patientTitle = patientTitle;
        this.patientName = patientName;
        this.patientDob = patientDob;
        this.patientGender = patientGender;
        this.patientMobile = patientMobile;
        this.billType = billType;
        this.status = status;
        this.billCanceled = billCanceled;
        this.createdAt = createdAt;
        this.barcodeGenerated = barcodeGenerated;
        this.barcodeGeneratedAt = barcodeGeneratedAt;
        this.sampleCollected = sampleCollected;
        this.sampleCollectedAt = sampleCollectedAt;
        this.sampleSent = sampleSent;
        this.sampleSentAt = sampleSentAt;
        this.sampleReceivedAtLab = sampleReceivedAtLab;
        this.sampleReceivedAtLabAt = sampleReceivedAtLabAt;
        this.cancelled = cancelled;
        this.cancelledAt = cancelledAt;
        this.sampleRejected = sampleRejected;
        this.sampleRejectedAt = sampleRejectedAt;
    }

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public Date getSampleDate() {
        return sampleDate;
    }

    public void setSampleDate(Date sampleDate) {
        this.sampleDate = sampleDate;
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

    public String getAnalyzerName() {
        return analyzerName;
    }

    public void setAnalyzerName(String analyzerName) {
        this.analyzerName = analyzerName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSampleTube() {
        return sampleTube;
    }

    public void setSampleTube(String sampleTube) {
        this.sampleTube = sampleTube;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
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

    public Boolean getBarcodeGenerated() {
        return barcodeGenerated;
    }

    public void setBarcodeGenerated(Boolean barcodeGenerated) {
        this.barcodeGenerated = barcodeGenerated;
    }

    public Date getBarcodeGeneratedAt() {
        return barcodeGeneratedAt;
    }

    public void setBarcodeGeneratedAt(Date barcodeGeneratedAt) {
        this.barcodeGeneratedAt = barcodeGeneratedAt;
    }

    public Boolean getSampleCollected() {
        return sampleCollected;
    }

    public void setSampleCollected(Boolean sampleCollected) {
        this.sampleCollected = sampleCollected;
    }

    public Date getSampleCollectedAt() {
        return sampleCollectedAt;
    }

    public void setSampleCollectedAt(Date sampleCollectedAt) {
        this.sampleCollectedAt = sampleCollectedAt;
    }

    public Boolean getSampleSent() {
        return sampleSent;
    }

    public void setSampleSent(Boolean sampleSent) {
        this.sampleSent = sampleSent;
    }

    public Date getSampleSentAt() {
        return sampleSentAt;
    }

    public void setSampleSentAt(Date sampleSentAt) {
        this.sampleSentAt = sampleSentAt;
    }

    public Boolean getSampleReceivedAtLab() {
        return sampleReceivedAtLab;
    }

    public void setSampleReceivedAtLab(Boolean sampleReceivedAtLab) {
        this.sampleReceivedAtLab = sampleReceivedAtLab;
    }

    public Date getSampleReceivedAtLabAt() {
        return sampleReceivedAtLabAt;
    }

    public void setSampleReceivedAtLabAt(Date sampleReceivedAtLabAt) {
        this.sampleReceivedAtLabAt = sampleReceivedAtLabAt;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Boolean getSampleRejected() {
        return sampleRejected;
    }

    public void setSampleRejected(Boolean sampleRejected) {
        this.sampleRejected = sampleRejected;
    }

    public Date getSampleRejectedAt() {
        return sampleRejectedAt;
    }

    public void setSampleRejectedAt(Date sampleRejectedAt) {
        this.sampleRejectedAt = sampleRejectedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public Title getPatientTitle() {
        return patientTitle;
    }

    public void setPatientTitle(Title patientTitle) {
        this.patientTitle = patientTitle;
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

    public String getPatientAge() {
        patientAge = LaboratoryCommonController.calculateAge(patientDob,billDate);
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }
    
    @Inject
    LaboratoryCommonController laboratoryCommonController;

    public String getCollectingCenterName() {
        collectingCenterName = laboratoryCommonController.getCollectingCentrNameFromSampleID(sampleId);
        return collectingCenterName;
    }

    public void setCollectingCenterName(String collectingCenterName) {
        this.collectingCenterName = collectingCenterName;
    }

}
