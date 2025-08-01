package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class SampleDTO implements Serializable {
    private Long id;
    private Long sampleId;
    private Date sampleDate;
    private String billNumber;
    private String patientName;
    private String analyzerName;

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
  
}
