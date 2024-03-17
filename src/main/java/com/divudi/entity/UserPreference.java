/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.OpdBillingStrategy;
import com.divudi.data.ItemListingStrategy;
import com.divudi.data.OpdTokenNumberGenerationStrategy;
import com.divudi.data.PaperType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.RestAuthenticationType;
import java.io.Serializable;
import javax.persistence.Column;
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
    private WebUser webUser;
    @ManyToOne
    private Department department;
    @ManyToOne
    private Institution institution;

    /*
    EHR
     */
    private String abbreviationForHistory;
    private String abbreviationForExamination;
    private String abbreviationForInvestigations;
    private String abbreviationForTreatments;
    private String abbreviationForManagement;

    /*
    Pharmacy
     */
    @Lob
    private String pharmacyRetailBillTemplate;
    @Lob
    private String pharmacyWholesaleBillTemplate;

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
    private String channellingBillTemplate;

    @Lob
    private String channellingCancellationBillTemplate;
    @Lob
    private String channelingDoctorPaymentBillTemplate;

    /*
    OPD
     */
    @Lob
    private String opdBillForCashierTemplate;
    @Lob
    private String smsTemplateForOpdBillSetting;

    @Deprecated
    boolean institutionSpecificItems = false;

    @Lob
    private String opdBillTemplate;
    @Deprecated
    private boolean institutionRestrictedBilling = false;
    private boolean opdSettleWithoutReferralDetails;
    private boolean partialPaymentOfOpdBillsAllowed;
    private boolean partialPaymentOfOpdPreBillsAllowed;
    private boolean paymentMethodAllowedInInwardMatrix;
    private boolean pharmacyBillPrabodha;
    private boolean checkPaymentSchemeValidation;
    private boolean grnBillDetailed;
    private boolean bhtNumberWithYear;
    private boolean bhtNumberWithOutAdmissionType;
    private boolean depNumGenFromToDepartment;
    private boolean tranferNetTotalbyRetailRate;
    private boolean allowtoChangePaymentMethodDuringPayment;
    private boolean opdSettleWithoutCashTendered;
    private boolean channelWithOutReferenceNumber;
    private boolean pharmayPurchaseWithLastRate;
    private boolean inwardAddServiceBillTimeCheck;
    private boolean inwardMoChargeCalculateInitialTime;
    private boolean inwardChangeAdmissionFee;
    private boolean pharmacyBillWithOutItem;
    private boolean fiveFivePaperWithHeadings;
    private boolean showOnlyMarkedDoctors = false;
    private boolean channelSettleWithoutPatientPhoneNumber = false;
    private boolean opdSettleWithoutPatientPhoneNumber = false;
    private boolean channelBillDouble = false;
    private boolean hasAwebsiteAsFrontEnd = false;
    private String themeName;
    private String logoUrl;
    private String loggingHeader;
    @Lob
    private String loggingText;
    private boolean channelDoctorArivalMsgSend = false;
    private String microBiologyFont;
    private String logoName;
    @Enumerated(EnumType.STRING)
    private PaperType opdBillPaperType;
    @Enumerated(EnumType.STRING)
    private PaperType inwardServiceBillPaperType;
    @Enumerated(EnumType.STRING)
    private PaperType pharmacyBillPaperType;
    @Enumerated(EnumType.STRING)
    private PaperType channelBillPaperType;

    @Enumerated(EnumType.STRING)
    private PaperType inwardDepositPaymentBillPaper;

    private boolean partialPaymentOfPharmacyBillsAllowed;

    @Deprecated
    @Enumerated(EnumType.STRING)
    private ApplicationInstitution applicationInstitution;
    @Enumerated(EnumType.STRING)
    private PaymentMethod channellingPaymentMethod;

    private Boolean canSettleOpdBillWithInvestigationsWithoutReferringDoctor;
    private Boolean printBarcodeInOpdBill;
    private Boolean sentEmailWithInvestigationReportApproval;
    private Boolean sentSmsWithInvestigationRequestApproval;
    private Boolean sentDailySmsSummeryForReferringDoctors;

    @Column(length = 255) // Adjust the length as needed
    private String smsUrl;

    @Column(length = 100) // Adjust the length as needed
    private String smsUsername;

    @Column(length = 100) // Adjust the length as needed
    private String smsPassword;

    @Column(length = 100) // Adjust the length as needed
    private String smsUserAlias;

    @Column(length = 100) // Adjust the length as needed
    private String smsUsernameParameterName;

    @Column(length = 100) // Adjust the length as needed
    private String smsPasswordParameterName;

    @Column(length = 100) // Adjust the length as needed
    private String smsUserAliasParameterName;

    @Column(length = 100) // Adjust the length as needed
    private String smsPhoneNumberParameterName;

    @Column(length = 100) // Adjust the length as needed
    private String smsMessageParameterName;

    @Enumerated
    private RestAuthenticationType smsAuthenticationType;

    private boolean familyMembership;
    private boolean membershipExpires;

    private boolean needAreaForPatientRegistration;
    private boolean needNicForPatientRegistration;
    private boolean needPhoneNumberForPatientRegistration;

    private boolean channellingSendSmsOnBooking;
    private boolean channellingSendSmsOnCancelling;
    private boolean channellingSendSmsOnArrival;
    private boolean sendBulkSms;
    private String pharmacyBillFooter;
    private String pharmacyBillHeader;
    private String longDateFormat;
    private String shortDateFormat;
    private String longDateTimeFormat;
    private String shortDateTimeFormat;
    private String longTimeFormat;
    private String shortTimeFormat;
    private String encrptionKey;

    private String nameRegex;
    private String mobileRegex;
    private String emailRegex;
    private String nicRegex;

    private String lengthOfOTPIndexes;

    @Enumerated(value = EnumType.STRING)
    private ItemListingStrategy opdItemListingStrategy;

    @Enumerated(value = EnumType.STRING)
    private ItemListingStrategy ccItemListingStrategy;

    @Enumerated(value = EnumType.STRING)
    private OpdBillingStrategy opdBillingStrategy;

    @Enumerated(value = EnumType.STRING)
    private OpdTokenNumberGenerationStrategy opdTokenNumberGenerationStrategy;
    private boolean printOpdTokenNumber = true;

    private boolean autodisplayMenu = true;
    //User Preference for Financial transaction manager
    private boolean showBillWiseDetails;
    @Lob
    private String inpatientFinalBillPrintHeader;
    private String changeTextCasesPatientName;
    
    @Lob
    private String smsTemplateForChannelBooking;

    private boolean opdSettleWithoutPatientArea;

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
        if (partialPaymentOfOpdBillsAllowed == true) {
            this.opdSettleWithoutCashTendered = true;
        } else {
            this.opdSettleWithoutCashTendered = false;
        }
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

    public String getPharmacyBillFooter() {
        return pharmacyBillFooter;
    }

    public void setPharmacyBillFooter(String pharmacyBillFooter) {
        this.pharmacyBillFooter = pharmacyBillFooter;
    }

    public String getPharmacyBillHeader() {
        return pharmacyBillHeader;
    }

    public void setPharmacyBillHeader(String pharmacyBillHeader) {
        this.pharmacyBillHeader = pharmacyBillHeader;
    }

    public String getSmsUrl() {
        return smsUrl;
    }

    public void setSmsUrl(String smsUrl) {
        this.smsUrl = smsUrl;
    }

    public String getSmsUsername() {
        return smsUsername;
    }

    public void setSmsUsername(String smsUsername) {
        this.smsUsername = smsUsername;
    }

    public String getSmsPassword() {
        return smsPassword;
    }

    public void setSmsPassword(String smsPassword) {
        this.smsPassword = smsPassword;
    }

    public String getSmsUsernameParameterName() {
        return smsUsernameParameterName;
    }

    public void setSmsUsernameParameterName(String smsUsernameParameterName) {
        this.smsUsernameParameterName = smsUsernameParameterName;
    }

    public String getSmsPasswordParameterName() {
        return smsPasswordParameterName;
    }

    public void setSmsPasswordParameterName(String smsPasswordParameterName) {
        this.smsPasswordParameterName = smsPasswordParameterName;
    }

    public String getSmsUserAlias() {
        return smsUserAlias;
    }

    public void setSmsUserAlias(String smsUserAlias) {
        this.smsUserAlias = smsUserAlias;
    }

    public String getSmsUserAliasParameterName() {
        return smsUserAliasParameterName;
    }

    public void setSmsUserAliasParameterName(String smsUserAliasParameterName) {
        this.smsUserAliasParameterName = smsUserAliasParameterName;
    }

    public String getSmsPhoneNumberParameterName() {
        return smsPhoneNumberParameterName;
    }

    public void setSmsPhoneNumberParameterName(String smsPhoneNumberParameterName) {
        this.smsPhoneNumberParameterName = smsPhoneNumberParameterName;
    }

    public String getSmsMessageParameterName() {
        return smsMessageParameterName;
    }

    public void setSmsMessageParameterName(String smsMessageParameterName) {
        this.smsMessageParameterName = smsMessageParameterName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getLoggingHeader() {
        return loggingHeader;
    }

    public void setLoggingHeader(String loggingHeader) {
        this.loggingHeader = loggingHeader;
    }

    public String getLoggingText() {
        return loggingText;
    }

    public void setLoggingText(String loggingText) {
        this.loggingText = loggingText;
    }

    public RestAuthenticationType getSmsAuthenticationType() {
        return smsAuthenticationType;
    }

    public void setSmsAuthenticationType(RestAuthenticationType smsAuthenticationType) {
        this.smsAuthenticationType = smsAuthenticationType;
    }

    public String getSmsTemplateForOpdBillSetting() {
        return smsTemplateForOpdBillSetting;
    }

    public void setSmsTemplateForOpdBillSetting(String smsTemplateForOpdBillSetting) {
        this.smsTemplateForOpdBillSetting = smsTemplateForOpdBillSetting;
    }

    public PaperType getInwardServiceBillPaperType() {
        if (inwardServiceBillPaperType == null) {
            inwardServiceBillPaperType = PaperType.FiveFivePaper;
        }
        return inwardServiceBillPaperType;
    }

    public void setInwardServiceBillPaperType(PaperType inwardServiceBillPaperType) {
        this.inwardServiceBillPaperType = inwardServiceBillPaperType;
    }

    public String getLongDateFormat() {
        if (longDateFormat == null || longDateFormat.trim().equals("")) {
            longDateFormat = "dd MMMM yyyy";
        }
        return longDateFormat;
    }

    public void setLongDateFormat(String longDateFormat) {
        this.longDateFormat = longDateFormat;
    }

    public String getShortDateFormat() {
        if (shortDateFormat == null || shortDateFormat.trim().equals("")) {
            shortDateFormat = "dd MMM yy";
        }
        return shortDateFormat;
    }

    public void setShortDateFormat(String shortDateFormat) {
        this.shortDateFormat = shortDateFormat;
    }

    public String getLongDateTimeFormat() {
        if (longDateTimeFormat == null || longDateTimeFormat.trim().equals("")) {
            longDateTimeFormat = "dd MMM yyyy HH:mm:ss";
        }
        return longDateTimeFormat;
    }

    public void setLongDateTimeFormat(String longDateTimeFormat) {
        this.longDateTimeFormat = longDateTimeFormat;
    }

    public String getShortDateTimeFormat() {
        if (shortDateTimeFormat == null || shortDateTimeFormat.trim().equals("")) {
            shortDateTimeFormat = "dd MMM yy HH:mm";
        }
        return shortDateTimeFormat;
    }

    public void setShortDateTimeFormat(String shortDateTimeFormat) {
        this.shortDateTimeFormat = shortDateTimeFormat;
    }

    public String getLongTimeFormat() {
        if (longTimeFormat == null || longTimeFormat.trim().equals("")) {
            longTimeFormat = "HH:mm:ss";
        }
        return longTimeFormat;
    }

    public void setLongTimeFormat(String longTimeFormat) {
        this.longTimeFormat = longTimeFormat;
    }

    public String getShortTimeFormat() {
        if (shortTimeFormat == null || shortTimeFormat.trim().equals("")) {
            shortTimeFormat = "HH:mm";
        }
        return shortTimeFormat;
    }

    public void setShortTimeFormat(String shortTimeFormat) {
        this.shortTimeFormat = shortTimeFormat;
    }

    public String getEncrptionKey() {
        return encrptionKey;
    }

    public void setEncrptionKey(String encrptionKey) {
        this.encrptionKey = encrptionKey;
    }

    public ItemListingStrategy getOpdItemListingStrategy() {
        if (opdItemListingStrategy == null) {
            opdItemListingStrategy = ItemListingStrategy.ALL_ITEMS;
        }
        return opdItemListingStrategy;
    }

    public void setOpdItemListingStrategy(ItemListingStrategy opdItemListingStrategy) {
        this.opdItemListingStrategy = opdItemListingStrategy;
    }

    public String getNameRegex() {
        return nameRegex;
    }

    public void setNameRegex(String nameRegex) {
        this.nameRegex = nameRegex;
    }

    public String getMobileRegex() {
        return mobileRegex;
    }

    public void setMobileRegex(String mobileRegex) {
        this.mobileRegex = mobileRegex;
    }

    public String getEmailRegex() {
        return emailRegex;
    }

    public void setEmailRegex(String emailRegex) {
        this.emailRegex = emailRegex;
    }

    public ItemListingStrategy getCcItemListingStrategy() {
        if (ccItemListingStrategy == null) {
            ccItemListingStrategy = ItemListingStrategy.ALL_ITEMS;
        }
        return ccItemListingStrategy;
    }

    public void setCcItemListingStrategy(ItemListingStrategy ccItemListingStrategy) {
        this.ccItemListingStrategy = ccItemListingStrategy;
    }

    public String getNicRegex() {
        return nicRegex;
    }

    public void setNicRegex(String nicRegex) {
        this.nicRegex = nicRegex;
    }

    public boolean isAutodisplayMenu() {
        return autodisplayMenu;
    }

    public void setAutodisplayMenu(boolean autodisplayMenu) {
        this.autodisplayMenu = autodisplayMenu;
    }

    public OpdTokenNumberGenerationStrategy getOpdTokenNumberGenerationStrategy() {
        if (opdTokenNumberGenerationStrategy == null) {
            opdTokenNumberGenerationStrategy = OpdTokenNumberGenerationStrategy.BILLS_BY_DEPARTMENT_CATEGORY_AND_FROMSTAFF;
        }
        return opdTokenNumberGenerationStrategy;
    }

    public void setOpdTokenNumberGenerationStrategy(OpdTokenNumberGenerationStrategy opdTokenNumberGenerationStrategy) {
        this.opdTokenNumberGenerationStrategy = opdTokenNumberGenerationStrategy;
    }

    public OpdBillingStrategy getOpdBillingStrategy() {
        if (opdBillingStrategy == null) {
            opdBillingStrategy = OpdBillingStrategy.ONE_BILL_PER_DEPARTMENT;
        }
        return opdBillingStrategy;
    }

    public void setOpdBillingStrategy(OpdBillingStrategy opdBillingStrategy) {
        this.opdBillingStrategy = opdBillingStrategy;
    }

    public boolean isPrintOpdTokenNumber() {
        return printOpdTokenNumber;
    }

    public void setPrintOpdTokenNumber(boolean printOpdTokenNumber) {
        this.printOpdTokenNumber = printOpdTokenNumber;
    }

    public boolean isBhtNumberWithOutAdmissionType() {
        return bhtNumberWithOutAdmissionType;
    }

    public void setBhtNumberWithOutAdmissionType(boolean bhtNumberWithOutAdmissionType) {
        this.bhtNumberWithOutAdmissionType = bhtNumberWithOutAdmissionType;
    }

    public boolean isShowBillWiseDetails() {
        return showBillWiseDetails;
    }

    public void setShowBillWiseDetails(boolean showBillWiseDetails) {
        this.showBillWiseDetails = showBillWiseDetails;
    }

    public String getInpatientFinalBillPrintHeader() {
        return inpatientFinalBillPrintHeader;
    }

    public void setInpatientFinalBillPrintHeader(String inpatientFinalBillPrintHeader) {
        this.inpatientFinalBillPrintHeader = inpatientFinalBillPrintHeader;
    }

    public String getChangeTextCasesPatientName() {
        return changeTextCasesPatientName;
    }

    public void setChangeTextCasesPatientName(String textCase) {
        this.changeTextCasesPatientName = textCase;
    }

    public PaperType getInwardDepositPaymentBillPaper() {
        if (inwardDepositPaymentBillPaper == null) {
            inwardDepositPaymentBillPaper = PaperType.A4Paper;
        }
        return inwardDepositPaymentBillPaper;
    }

    public void setInwardDepositPaymentBillPaper(PaperType inwardDepositPaymentBillPaper) {
        this.inwardDepositPaymentBillPaper = inwardDepositPaymentBillPaper;
    }

    public String getLengthOfOTPIndexes() {
        return lengthOfOTPIndexes;
    }

    public void setLengthOfOTPIndexes(String lengthOfOTPIndexes) {
        if (lengthOfOTPIndexes == null || lengthOfOTPIndexes.isEmpty()) {
            lengthOfOTPIndexes = "4";
        }
        this.lengthOfOTPIndexes = lengthOfOTPIndexes;
      
    }

    public boolean isPartialPaymentOfPharmacyBillsAllowed() {
        return partialPaymentOfPharmacyBillsAllowed;
    }

    public void setPartialPaymentOfPharmacyBillsAllowed(boolean partialPaymentOfPharmacyBillsAllowed) {
        this.partialPaymentOfPharmacyBillsAllowed = partialPaymentOfPharmacyBillsAllowed;
    }

    public String getSmsTemplateForChannelBooking() {
        return smsTemplateForChannelBooking;
    }

    public void setSmsTemplateForChannelBooking(String smsTemplateForChannelBooking) {
        this.smsTemplateForChannelBooking = smsTemplateForChannelBooking;
    }

    public boolean isOpdSettleWithoutPatientArea() {
        return opdSettleWithoutPatientArea;
    }

    public void setOpdSettleWithoutPatientArea(boolean opdSettleWithoutPatientArea) {
        this.opdSettleWithoutPatientArea = opdSettleWithoutPatientArea;
    }

    
    
    


}
