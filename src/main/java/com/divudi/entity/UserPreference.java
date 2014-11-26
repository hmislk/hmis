/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.PaperType;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

/**
 *
 * @author pdhs
 */
@Entity
public class UserPreference implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    String abbreviationForHistory;
    String abbreviationForExamination;
    String abbreviationForInvestigations;
    String abbreviationForTreatments;
    String abbreviationForManagement;
    @Lob
    String pharmacyBillFooter;
    @ManyToOne
    WebUser webUser;
    @ManyToOne
    Department department;
    @ManyToOne
    Institution institution;
    boolean printLabelForOPdBill;
    boolean partialPaymentOfOpdBillsAllowed;
    boolean paymentMethodAllowedInInwardMatrix;
    @Enumerated(EnumType.STRING)
    PaperType opdBillPaperType;

    public boolean isPaymentMethodAllowedInInwardMatrix() {
        return paymentMethodAllowedInInwardMatrix;
    }

    public void setPaymentMethodAllowedInInwardMatrix(boolean paymentMethodAllowedInInwardMatrix) {
        this.paymentMethodAllowedInInwardMatrix = paymentMethodAllowedInInwardMatrix;
    }

    
    
    public boolean isPartialPaymentOfOpdBillsAllowed() {
        return partialPaymentOfOpdBillsAllowed;
    }

    public void setPartialPaymentOfOpdBillsAllowed(boolean partialPaymentOfOpdBillsAllowed) {
        this.partialPaymentOfOpdBillsAllowed = partialPaymentOfOpdBillsAllowed;
    }
    
    

    public PaperType getOpdBillPaperType() {
        if(opdBillPaperType==null){
            opdBillPaperType = PaperType.FiveFivePaper;
        }
        return opdBillPaperType;
    }

    public void setOpdBillPaperType(PaperType opdBillPaperType) {
        this.opdBillPaperType = opdBillPaperType;
    }
    
    
    
    

    public String getAbbreviationForHistory() {
        if (abbreviationForHistory == null || "".equals(abbreviationForHistory)) {
            abbreviationForHistory = "Hx";
        }
        return abbreviationForHistory;
    }

    public void setAbbreviationForHistory(String abbreviationForHistory) {
        this.abbreviationForHistory = abbreviationForHistory;
    }

    public String getAbbreviationForExamination() {
        if (abbreviationForExamination == null || "".equals(abbreviationForExamination)) {
            abbreviationForExamination = "Ex";
        }
        return abbreviationForExamination;
    }

    public void setAbbreviationForExamination(String abbreviationForExamination) {
        this.abbreviationForExamination = abbreviationForExamination;
    }

    public String getAbbreviationForInvestigations() {
        if (abbreviationForInvestigations == null || "".equals(abbreviationForInvestigations)) {
            abbreviationForInvestigations = "Ix";
        }
        return abbreviationForInvestigations;
    }

    public void setAbbreviationForInvestigations(String abbreviationForInvestigations) {
        this.abbreviationForInvestigations = abbreviationForInvestigations;
    }

    public String getAbbreviationForTreatments() {
        if (abbreviationForTreatments == null || "".equals(abbreviationForTreatments)) {
            abbreviationForTreatments = "Rx";
        }
        return abbreviationForTreatments;
    }

    public void setAbbreviationForTreatments(String abbreviationForTreatments) {
        
        this.abbreviationForTreatments = abbreviationForTreatments;
    }

    public String getAbbreviationForManagement() {
        if (abbreviationForManagement == null || "".equals(abbreviationForTreatments)) {
            abbreviationForManagement = "Mx";
        }
        return abbreviationForManagement;
    }

    public void setAbbreviationForManagement(String abbreviationForManagement) {
        this.abbreviationForManagement = abbreviationForManagement;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isPrintLabelForOPdBill() {
        return printLabelForOPdBill;
    }

    public void setPrintLabelForOPdBill(boolean printLabelForOPdBill) {
        this.printLabelForOPdBill = printLabelForOPdBill;
    }

    public String getPharmacyBillFooter() {
        return pharmacyBillFooter;
    }

    public void setPharmacyBillFooter(String pharmacyBillFooter) {
        this.pharmacyBillFooter = pharmacyBillFooter;
    }
    
    
    
    

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserPreference)) {
            return false;
        }
        UserPreference other = (UserPreference) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.UserPreference[ id=" + id + " ]";
    }

}
