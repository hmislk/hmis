package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;
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
    private Date billDate;
    private String orderedInstitution;
    private String orderedDepaerment;
    private String investigationName;
    private String billStatus;
    
    //Patient Variable
    private String patientName;
    private String patientAge;
    private String patientGender;
    private String patientMobileNo;
    
    

    public LaboratoryDashboardDTO() {
        
    }

    public LaboratoryDashboardDTO(
            Long patientInvestigationId, 
            Long billId, 
            String billNumber, 
            Date billDate, 
            String orderedInstitution, 
            String orderedDepaerment, 
            String investigationName, 
            String billStatus, 
            String patientName, 
            String patientAge, 
            String patientGender, 
            String patientMobileNo) {
        this.patientInvestigationId = patientInvestigationId;
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.orderedInstitution = orderedInstitution;
        this.orderedDepaerment = orderedDepaerment;
        this.investigationName = investigationName;
        this.billStatus = billStatus;
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.patientGender = patientGender;
        this.patientMobileNo = patientMobileNo;
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

    public String getBillStatus() {
        return billStatus;
    }

    public void setBillStatus(String billStatus) {
        this.billStatus = billStatus;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getPatientMobileNo() {
        return patientMobileNo;
    }

    public void setPatientMobileNo(String patientMobileNo) {
        this.patientMobileNo = patientMobileNo;
    }

    
    
}
