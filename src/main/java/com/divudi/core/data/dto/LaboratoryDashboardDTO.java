package com.divudi.core.data.dto;

import com.divudi.core.data.lab.PatientInvestigationStatus;
import java.io.Serializable;
import java.util.Date;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public class LaboratoryDashboardDTO implements Serializable {

    //Bill Variable
    private Long patientInvestigationId;
    private Long billId;
    private String billNumber;
    private String bhtNo;
    private Date billDate;
    private String orderedInstitution;
    private String orderedDepaerment;
    private String collectingCentre;
    private String investigationName;
    private PatientInvestigationStatus billStatus;
    
    //Patient Variable
    private Long patientId;

    public LaboratoryDashboardDTO() {
        
    }

//  Use for Laboratory Doctor Dashboard
//    1. List Investigtion
//    2. List Patient Report
    public LaboratoryDashboardDTO(
            Long patientInvestigationId, 
            Long billId, 
            String billNumber, 
            String bhtNo,
            Date billDate, 
            String orderedInstitution, 
            String orderedDepaerment, 
            String collectingCentre, 
            String investigationName, 
            PatientInvestigationStatus billStatus, 
            Long patientId) {
        this.patientInvestigationId = patientInvestigationId;
        this.billId = billId;
        this.billNumber = billNumber;
        this.bhtNo = bhtNo;
        this.billDate = billDate;
        this.orderedInstitution = orderedInstitution;
        this.orderedDepaerment = orderedDepaerment;
        this.collectingCentre = collectingCentre;
        this.investigationName = investigationName;
        this.billStatus = billStatus;
        this.patientId = patientId;
    }
    
    
    
    
    

    public Long getPatientInvestigationId() {
        return patientInvestigationId;
    }

    public void setPatientInvestigationId(Long patientInvestigationId) {
        this.patientInvestigationId = patientInvestigationId;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getOrderedInstitution() {
        return orderedInstitution;
    }

    public void setOrderedInstitution(String orderedInstitution) {
        this.orderedInstitution = orderedInstitution;
    }

    public String getOrderedDepaerment() {
        return orderedDepaerment;
    }

    public void setOrderedDepaerment(String orderedDepaerment) {
        this.orderedDepaerment = orderedDepaerment;
    }

    public String getInvestigationName() {
        return investigationName;
    }

    public void setInvestigationName(String investigationName) {
        this.investigationName = investigationName;
    }

    public String getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(String collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public PatientInvestigationStatus getBillStatus() {
        return billStatus;
    }

    public void setBillStatus(PatientInvestigationStatus billStatus) {
        this.billStatus = billStatus;
    }

}
