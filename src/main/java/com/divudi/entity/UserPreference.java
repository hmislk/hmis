/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.PaperType;
import com.divudi.data.PaymentMethod;
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
    String pharmacyBillHeader;
    @Lob
    String pharmacyBillFooter;
    @Lob
    String opdBillHeader;
    @Lob
    String opdBillFooter;
    @Lob
    String channellingBillHeader;
    @Lob
    String channellingBillFooter;
    
    @ManyToOne
    WebUser webUser;
    @ManyToOne
    Department department;
    @ManyToOne
    Institution institution;
    boolean institutionSpecificItems=false;
    boolean printLabelForOPdBill;
    boolean partialPaymentOfOpdBillsAllowed;
    boolean partialPaymentOfOpdPreBillsAllowed;
    boolean paymentMethodAllowedInInwardMatrix;
    boolean pharmacyBillPrabodha;
    boolean checkPaymentSchemeValidation;
    boolean grnBillDetailed;
    boolean bhtNumberWithYear;
    boolean depNumGenFromToDepartment;
    boolean tranferNetTotalbyRetailRate;
    boolean allowtoChangePaymentMethodDuringPayment;
    boolean opdPosBillWithoutLogo;
    boolean channelWithOutReferenceNumber;
    boolean pharmayPurchaseWithLastRate;
    boolean inwardAddServiceBillTimeCheck;
    boolean inwardMoChargeCalculateInitialTime;
    boolean inwardChangeAdmissionFee;
    boolean pharmacyBillWithOutItem;
    boolean fiveFivePaperWithHeadings;
    boolean showOnlyMarkedDoctors=false;
    String microBiologyFont;
    String logoName;
    @Enumerated(EnumType.STRING)
    PaperType opdBillPaperType;
    @Enumerated(EnumType.STRING)
    PaperType pharmacyBillPaperType;
    @Enumerated(EnumType.STRING)
    ApplicationInstitution applicationInstitution;
    @Enumerated(EnumType.STRING)
    PaymentMethod channellingPaymentMethod;

    public ApplicationInstitution getApplicationInstitution() {
        if(applicationInstitution==null){
            applicationInstitution = ApplicationInstitution.Ruhuna;
        }
        return applicationInstitution;
    }

    public void setApplicationInstitution(ApplicationInstitution applicationInstitution) {
        this.applicationInstitution = applicationInstitution;
    }   
    

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

    public boolean isPharmacyBillPrabodha() {
        return pharmacyBillPrabodha;
    }

    public void setPharmacyBillPrabodha(boolean pharmacyBillPrabodha) {
        this.pharmacyBillPrabodha = pharmacyBillPrabodha;
    }

    public boolean isPharmayPurchaseWithLastRate() {
        return pharmayPurchaseWithLastRate;
    }

    public void setPharmayPurchaseWithLastRate(boolean pharmayPurchaseWithLastRate) {
        this.pharmayPurchaseWithLastRate = pharmayPurchaseWithLastRate;
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

    public PaperType getPharmacyBillPaperType() {
        return pharmacyBillPaperType;
    }

    public void setPharmacyBillPaperType(PaperType pharmacyBillPaperType) {
        this.pharmacyBillPaperType = pharmacyBillPaperType;
    }

    public boolean isCheckPaymentSchemeValidation() {
        return checkPaymentSchemeValidation;
    }

    public void setCheckPaymentSchemeValidation(boolean checkPaymentSchemeValidation) {
        this.checkPaymentSchemeValidation = checkPaymentSchemeValidation;
    }

    public boolean isBhtNumberWithYear() {
        return bhtNumberWithYear;
    }

    public void setBhtNumberWithYear(boolean bhtNumberWithYear) {
        this.bhtNumberWithYear = bhtNumberWithYear;
    }

    public boolean isAllowtoChangePaymentMethodDuringPayment() {
        return allowtoChangePaymentMethodDuringPayment;
    }

    public void setAllowtoChangePaymentMethodDuringPayment(boolean allowtoChangePaymentMethodDuringPayment) {
        this.allowtoChangePaymentMethodDuringPayment = allowtoChangePaymentMethodDuringPayment;
    }

    public boolean isOpdPosBillWithoutLogo() {
        return opdPosBillWithoutLogo;
    }

    public void setOpdPosBillWithoutLogo(boolean opdPosBillWithoutLogo) {
        this.opdPosBillWithoutLogo = opdPosBillWithoutLogo;
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

    public boolean isInstitutionSpecificItems() {
        return institutionSpecificItems;
    }

    public void setInstitutionSpecificItems(boolean institutionSpecificItems) {
        this.institutionSpecificItems = institutionSpecificItems;
    }
    
    

    public boolean isPrintLabelForOPdBill() {
        return printLabelForOPdBill;
    }

    public void setPrintLabelForOPdBill(boolean printLabelForOPdBill) {
        this.printLabelForOPdBill = printLabelForOPdBill;
    }

    public boolean isGrnBillDetailed() {
        return grnBillDetailed;
    }

    public void setGrnBillDetailed(boolean grnBillDetailed) {
        this.grnBillDetailed = grnBillDetailed;
    }

    public String getMicroBiologyFont() {
        return microBiologyFont;
    }

    public void setMicroBiologyFont(String microBiologyFont) {
        this.microBiologyFont = microBiologyFont;
    }

    
    
    

    public String getPharmacyBillFooter() {
        return pharmacyBillFooter;
    }

    public void setPharmacyBillFooter(String pharmacyBillFooter) {
        this.pharmacyBillFooter = pharmacyBillFooter;
    }

    public boolean isDepNumGenFromToDepartment() {
        return depNumGenFromToDepartment;
    }

    public void setDepNumGenFromToDepartment(boolean depNumGenFromToDepartment) {
        this.depNumGenFromToDepartment = depNumGenFromToDepartment;
    }

    public boolean isTranferNetTotalbyRetailRate() {
        return tranferNetTotalbyRetailRate;
    }

    public void setTranferNetTotalbyRetailRate(boolean tranferNetTotalbyRetailRate) {
        this.tranferNetTotalbyRetailRate = tranferNetTotalbyRetailRate;
    }

    public String getLogoName() {
        if (logoName == null || "".equals(logoName)) {
            logoName = null;
        }
        return logoName;
    }

    public void setLogoName(String logoName) {
        this.logoName = logoName;
    }

    public boolean isChannelWithOutReferenceNumber() {
        return channelWithOutReferenceNumber;
    }

    public void setChannelWithOutReferenceNumber(boolean channelWithOutReferenceNumber) {
        this.channelWithOutReferenceNumber = channelWithOutReferenceNumber;
    }

    public boolean isInwardAddServiceBillTimeCheck() {
        return inwardAddServiceBillTimeCheck;
    }

    public void setInwardAddServiceBillTimeCheck(boolean inwardAddServiceBillTimeCheck) {
        this.inwardAddServiceBillTimeCheck = inwardAddServiceBillTimeCheck;
    }
    
    public boolean isInwardMoChargeCalculateInitialTime() {
        return inwardMoChargeCalculateInitialTime;
    }

    public void setInwardMoChargeCalculateInitialTime(boolean inwardMoChargeCalculateInitialTime) {
        this.inwardMoChargeCalculateInitialTime = inwardMoChargeCalculateInitialTime;
    }

    public boolean isInwardChangeAdmissionFee() {
        return inwardChangeAdmissionFee;
    }

    public void setInwardChangeAdmissionFee(boolean inwardChangeAdmissionFee) {
        this.inwardChangeAdmissionFee = inwardChangeAdmissionFee;
    }

    public boolean isPartialPaymentOfOpdPreBillsAllowed() {
        return partialPaymentOfOpdPreBillsAllowed;
    }

    public void setPartialPaymentOfOpdPreBillsAllowed(boolean partialPaymentOfOpdPreBillsAllowed) {
        this.partialPaymentOfOpdPreBillsAllowed = partialPaymentOfOpdPreBillsAllowed;
    }

    public boolean isPharmacyBillWithOutItem() {
        return pharmacyBillWithOutItem;
    }

    public void setPharmacyBillWithOutItem(boolean pharmacyBillWithOutItem) {
        this.pharmacyBillWithOutItem = pharmacyBillWithOutItem;
    }

    public boolean isFiveFivePaperWithHeadings() {
        return fiveFivePaperWithHeadings;
    }

    public void setFiveFivePaperWithHeadings(boolean fiveFivePaperWithHeadings) {
        this.fiveFivePaperWithHeadings = fiveFivePaperWithHeadings;
    }

    public boolean isShowOnlyMarkedDoctors() {
        return showOnlyMarkedDoctors;
    }

    public void setShowOnlyMarkedDoctors(boolean showOnlyMarkedDoctors) {
        this.showOnlyMarkedDoctors = showOnlyMarkedDoctors;
    }

    public String getPharmacyBillHeader() {
        return pharmacyBillHeader;
    }

    public void setPharmacyBillHeader(String pharmacyBillHeader) {
        this.pharmacyBillHeader = pharmacyBillHeader;
    }

    public String getOpdBillHeader() {
        return opdBillHeader;
    }

    public void setOpdBillHeader(String opdBillHeader) {
        this.opdBillHeader = opdBillHeader;
    }

    public String getOpdBillFooter() {
        return opdBillFooter;
    }

    public void setOpdBillFooter(String opdBillFooter) {
        this.opdBillFooter = opdBillFooter;
    }

    public String getChannellingBillHeader() {
        return channellingBillHeader;
    }

    public void setChannellingBillHeader(String channellingBillHeader) {
        this.channellingBillHeader = channellingBillHeader;
    }

    public String getChannellingBillFooter() {
        return channellingBillFooter;
    }

    public void setChannellingBillFooter(String channellingBillFooter) {
        this.channellingBillFooter = channellingBillFooter;
    }

    public PaymentMethod getChannellingPaymentMethod() {
        if(channellingPaymentMethod==null){
            channellingPaymentMethod = PaymentMethod.OnCall;
        }
        return channellingPaymentMethod;
    }

    public void setChannellingPaymentMethod(PaymentMethod channellingPaymentMethod) {
        this.channellingPaymentMethod = channellingPaymentMethod;
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
