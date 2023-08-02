/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
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
    
    /*
    Owner
    */
    @ManyToOne
    WebUser webUser;
    @ManyToOne
    Department department;
    @ManyToOne
    Institution institution;
    
    /*
    EHR
    */
    String abbreviationForHistory;
    String abbreviationForExamination;
    String abbreviationForInvestigations;
    String abbreviationForTreatments;
    String abbreviationForManagement;
    
    
    /*
    Pharmacy
    */
    @Lob
    String pharmacyRetailBillTemplate;
    @Lob
    String pharmacyWholesaleBillTemplate;
    
    
    /*
    Inpatients
    */
    @Lob
    private String inwardDepositBillTemplate;
    @Lob
    private String inwardDepositCancelBillTemplate;
    
    /*
    Channelling
    */
    
    @Lob
    String channellingBillTemplate;
    
    
   
    
    @Lob
    String channellingCancellationBillTemplate;
    @Lob
    String channelingDoctorPaymentBillTemplate;
    
    
    /*
    OPD
    */
    @Lob
    String opdBillForCashierTemplate;

    
    boolean institutionSpecificItems = false;
    @Lob
    private String opdBillTemplate;
    private boolean institutionRestrictedBilling = false;
    boolean opdSettleWithoutReferralDetails;
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
    boolean opdSettleWithoutCashTendered;
    boolean channelWithOutReferenceNumber;
    boolean pharmayPurchaseWithLastRate;
    boolean inwardAddServiceBillTimeCheck;
    boolean inwardMoChargeCalculateInitialTime;
    boolean inwardChangeAdmissionFee;
    boolean pharmacyBillWithOutItem;
    boolean fiveFivePaperWithHeadings;
    boolean showOnlyMarkedDoctors = false;
    boolean channelSettleWithoutPatientPhoneNumber = false;
    boolean opdSettleWithoutPatientPhoneNumber = false;
    boolean channelBillDouble = false;
    private boolean hasAwebsiteAsFrontEnd = false;
    private String themeName;
    private boolean channelDoctorArivalMsgSend = false;
    String microBiologyFont;
    String logoName;
    @Enumerated(EnumType.STRING)
    PaperType opdBillPaperType;
    @Enumerated(EnumType.STRING)
    PaperType pharmacyBillPaperType;
    @Enumerated(EnumType.STRING)
    PaperType channelBillPaperType;
    @Enumerated(EnumType.STRING)
    ApplicationInstitution applicationInstitution;
    @Enumerated(EnumType.STRING)
    PaymentMethod channellingPaymentMethod;

    private Boolean canSettleOpdBillWithInvestigationsWithoutReferringDoctor;
    private Boolean printBarcodeInOpdBill;
    private Boolean sentEmailWithInvestigationReportApproval;
    private Boolean sentSmsWithInvestigationRequestApproval;
    private Boolean sentDailySmsSummeryForReferringDoctors;

    private boolean familyMembership;
    private boolean membershipExpires;

    private boolean needAreaForPatientRegistration;
    private boolean needNicForPatientRegistration;
    private boolean needPhoneNumberForPatientRegistration;

    private boolean channellingSendSmsOnBooking;
    private boolean channellingSendSmsOnCancelling;
    private boolean channellingSendSmsOnArrival;
    private boolean sendBulkSms;

    public ApplicationInstitution getApplicationInstitution() {
        if (applicationInstitution == null) {
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

    public boolean isChannelBillDouble() {
        return channelBillDouble;
    }

    public void setChannelBillDouble(boolean channelBillDouble) {
        this.channelBillDouble = channelBillDouble;
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
        if (opdBillPaperType == null) {
            opdBillPaperType = PaperType.FiveFivePaper;
        }
        return opdBillPaperType;
    }

    public void setOpdBillPaperType(PaperType opdBillPaperType) {
        this.opdBillPaperType = opdBillPaperType;
    }

    public PaperType getChannelBillPaperType() {
        return channelBillPaperType;
    }

    public void setChannelBillPaperType(PaperType channelBillPaperType) {
        if (opdBillPaperType == null) {
            opdBillPaperType = PaperType.Paper24_2x9_3;
        }
        this.channelBillPaperType = channelBillPaperType;
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

    public boolean isOpdSettleWithoutCashTendered() {
        return opdSettleWithoutCashTendered;
    }

    public void setOpdSettleWithoutCashTendered(boolean opdSettleWithoutCashTendered) {
        this.opdSettleWithoutCashTendered = opdSettleWithoutCashTendered;
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

    public boolean isOpdSettleWithoutReferralDetails() {
        return opdSettleWithoutReferralDetails;
    }

    public void setOpdSettleWithoutReferralDetails(boolean opdSettleWithoutReferralDetails) {
        this.opdSettleWithoutReferralDetails = opdSettleWithoutReferralDetails;
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

    public String getPharmacyWholesaleBillTemplate() {
        return pharmacyWholesaleBillTemplate;
    }

    public void setPharmacyWholesaleBillTemplate(String pharmacyWholesaleBillTemplate) {
        this.pharmacyWholesaleBillTemplate = pharmacyWholesaleBillTemplate;
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

    public String getPharmacyRetailBillTemplate() {
        return pharmacyRetailBillTemplate;
    }

    public void setPharmacyRetailBillTemplate(String pharmacyRetailBillTemplate) {
        this.pharmacyRetailBillTemplate = pharmacyRetailBillTemplate;
    }

    public String getChannellingBillTemplate() {
        return channellingBillTemplate;
    }

    public void setChannellingBillTemplate(String channellingBillTemplate) {
        this.channellingBillTemplate = channellingBillTemplate;
    }

    public String getOpdBillForCashierTemplate() {
        return opdBillForCashierTemplate;
    }

    public void setOpdBillForCashierTemplate(String opdBillForCashierTemplate) {
        this.opdBillForCashierTemplate = opdBillForCashierTemplate;
    }

    public String getChannellingCancellationBillTemplate() {
        return channellingCancellationBillTemplate;
    }

    public void setChannellingCancellationBillTemplate(String channellingCancellationBillTemplate) {
        this.channellingCancellationBillTemplate = channellingCancellationBillTemplate;
    }

    public String getChannelingDoctorPaymentBillTemplate() {
        return channelingDoctorPaymentBillTemplate;
    }

    public void setChannelingDoctorPaymentBillTemplate(String channelingDoctorPaymentBillTemplate) {
        this.channelingDoctorPaymentBillTemplate = channelingDoctorPaymentBillTemplate;
    }

    public PaymentMethod getChannellingPaymentMethod() {
        if (channellingPaymentMethod == null) {
            channellingPaymentMethod = PaymentMethod.OnCall;
        }
        return channellingPaymentMethod;
    }

    public void setChannellingPaymentMethod(PaymentMethod channellingPaymentMethod) {
        this.channellingPaymentMethod = channellingPaymentMethod;
    }

    public boolean isChannelSettleWithoutPatientPhoneNumber() {
        return channelSettleWithoutPatientPhoneNumber;
    }

    public void setChannelSettleWithoutPatientPhoneNumber(boolean channelSettleWithoutPatientPhoneNumber) {
        this.channelSettleWithoutPatientPhoneNumber = channelSettleWithoutPatientPhoneNumber;
    }

    public boolean isOpdSettleWithoutPatientPhoneNumber() {
        return opdSettleWithoutPatientPhoneNumber;
    }

    public void setOpdSettleWithoutPatientPhoneNumber(boolean opdSettleWithoutPatientPhoneNumber) {
        this.opdSettleWithoutPatientPhoneNumber = opdSettleWithoutPatientPhoneNumber;
    }

    public boolean isChannelDoctorArivalMsgSend() {
        return channelDoctorArivalMsgSend;
    }

    public void setChannelDoctorArivalMsgSend(boolean channelDoctorArivalMsgSend) {
        this.channelDoctorArivalMsgSend = channelDoctorArivalMsgSend;
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

    public boolean isInstitutionRestrictedBilling() {
        return institutionRestrictedBilling;
    }

    public void setInstitutionRestrictedBilling(boolean institutionRestrictedBilling) {
        this.institutionRestrictedBilling = institutionRestrictedBilling;
    }

    public Boolean getSentEmailWithInvestigationReportApproval() {
        if (sentEmailWithInvestigationReportApproval == null) {
            sentEmailWithInvestigationReportApproval = true;
        }
        return sentEmailWithInvestigationReportApproval;
    }

    public void setSentEmailWithInvestigationReportApproval(Boolean sentEmailWithInvestigationReportApproval) {
        this.sentEmailWithInvestigationReportApproval = sentEmailWithInvestigationReportApproval;
    }

    public Boolean getSentSmsWithInvestigationRequestApproval() {
        if (sentSmsWithInvestigationRequestApproval == null) {
            sentSmsWithInvestigationRequestApproval = true;
        }
        return sentSmsWithInvestigationRequestApproval;
    }

    public void setSentSmsWithInvestigationRequestApproval(Boolean sentSmsWithInvestigationRequestApproval) {
        this.sentSmsWithInvestigationRequestApproval = sentSmsWithInvestigationRequestApproval;
    }

    public Boolean getSentDailySmsSummeryForReferringDoctors() {
        if (sentDailySmsSummeryForReferringDoctors == null) {
            sentDailySmsSummeryForReferringDoctors = true;
        }
        return sentDailySmsSummeryForReferringDoctors;
    }

    public void setSentDailySmsSummeryForReferringDoctors(Boolean sentDailySmsSummeryForReferringDoctors) {
        this.sentDailySmsSummeryForReferringDoctors = sentDailySmsSummeryForReferringDoctors;
    }

    public Boolean getCanSettleOpdBillWithInvestigationsWithoutReferringDoctor() {
        if (canSettleOpdBillWithInvestigationsWithoutReferringDoctor == null) {
            canSettleOpdBillWithInvestigationsWithoutReferringDoctor = true;
        }
        return canSettleOpdBillWithInvestigationsWithoutReferringDoctor;
    }

    public void setCanSettleOpdBillWithInvestigationsWithoutReferringDoctor(Boolean canSettleOpdBillWithInvestigationsWithoutReferringDoctor) {
        this.canSettleOpdBillWithInvestigationsWithoutReferringDoctor = canSettleOpdBillWithInvestigationsWithoutReferringDoctor;
    }

    public Boolean getPrintBarcodeInOpdBill() {
        return printBarcodeInOpdBill;
    }

    public void setPrintBarcodeInOpdBill(Boolean printBarcodeInOpdBill) {
        this.printBarcodeInOpdBill = printBarcodeInOpdBill;
    }

    public boolean isFamilyMembership() {
        return familyMembership;
    }

    public void setFamilyMembership(boolean familyMembership) {
        this.familyMembership = familyMembership;
    }

    public boolean isMembershipExpires() {
        return membershipExpires;
    }

    public void setMembershipExpires(boolean membershipExpires) {
        this.membershipExpires = membershipExpires;
    }

    public boolean isNeedAreaForPatientRegistration() {
        return needAreaForPatientRegistration;
    }

    public void setNeedAreaForPatientRegistration(boolean needAreaForPatientRegistration) {
        this.needAreaForPatientRegistration = needAreaForPatientRegistration;
    }

    public boolean isNeedNicForPatientRegistration() {
        return needNicForPatientRegistration;
    }

    public void setNeedNicForPatientRegistration(boolean needNicForPatientRegistration) {
        this.needNicForPatientRegistration = needNicForPatientRegistration;
    }

    public boolean isNeedPhoneNumberForPatientRegistration() {
        return needPhoneNumberForPatientRegistration;
    }

    public void setNeedPhoneNumberForPatientRegistration(boolean needPhoneNumberForPatientRegistration) {
        this.needPhoneNumberForPatientRegistration = needPhoneNumberForPatientRegistration;
    }

    public boolean isChannellingSendSmsOnBooking() {
        return channellingSendSmsOnBooking;
    }

    public void setChannellingSendSmsOnBooking(boolean channellingSendSmsOnBooking) {
        this.channellingSendSmsOnBooking = channellingSendSmsOnBooking;
    }

    public boolean isChannellingSendSmsOnCancelling() {
        return channellingSendSmsOnCancelling;
    }

    public void setChannellingSendSmsOnCancelling(boolean channellingSendSmsOnCancelling) {
        this.channellingSendSmsOnCancelling = channellingSendSmsOnCancelling;
    }

    public boolean isChannellingSendSmsOnArrival() {
        return channellingSendSmsOnArrival;
    }

    public void setChannellingSendSmsOnArrival(boolean channellingSendSmsOnArrival) {
        this.channellingSendSmsOnArrival = channellingSendSmsOnArrival;
    }

    public boolean isSendBulkSms() {
        return sendBulkSms;
    }

    public void setSendBulkSms(boolean sendBulkSms) {
        this.sendBulkSms = sendBulkSms;
    }

    public String getInwardDepositBillTemplate() {
        return inwardDepositBillTemplate;
    }

    public void setInwardDepositBillTemplate(String inwardDepositBillTemplate) {
        this.inwardDepositBillTemplate = inwardDepositBillTemplate;
    }

    public String getInwardDepositCancelBillTemplate() {
        return inwardDepositCancelBillTemplate;
    }

    public void setInwardDepositCancelBillTemplate(String inwardDepositCancelBillTemplate) {
        this.inwardDepositCancelBillTemplate = inwardDepositCancelBillTemplate;
    }

    public boolean isHasAwebsiteAsFrontEnd() {
        return hasAwebsiteAsFrontEnd;
    }

    public void setHasAwebsiteAsFrontEnd(boolean hasAwebsiteAsFrontEnd) {
        this.hasAwebsiteAsFrontEnd = hasAwebsiteAsFrontEnd;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getOpdBillTemplate() {
        return opdBillTemplate;
    }

    public void setOpdBillTemplate(String opdBillTemplate) {
        this.opdBillTemplate = opdBillTemplate;
    }

}
