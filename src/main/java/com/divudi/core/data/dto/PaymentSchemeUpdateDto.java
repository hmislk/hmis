package com.divudi.core.data.dto;

public class PaymentSchemeUpdateDto {

    private String name;
    private String printingName;
    private Boolean validForPharmacy;
    private Boolean validForBilledBills;
    private Boolean validForInpatientBills;
    private Boolean validForChanneling;
    private Boolean staffMemberRequired;
    private Boolean membershipRequired;
    private Boolean staffRequired;
    private Boolean staffOrFamilyRequired;
    private Boolean memberRequired;
    private Boolean memberOrFamilyRequired;
    private Boolean seniorCitizenRequired;
    private Boolean pregnantMotherRequired;
    private Integer orderNo;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrintingName() { return printingName; }
    public void setPrintingName(String printingName) { this.printingName = printingName; }

    public Boolean getValidForPharmacy() { return validForPharmacy; }
    public void setValidForPharmacy(Boolean validForPharmacy) { this.validForPharmacy = validForPharmacy; }

    public Boolean getValidForBilledBills() { return validForBilledBills; }
    public void setValidForBilledBills(Boolean validForBilledBills) { this.validForBilledBills = validForBilledBills; }

    public Boolean getValidForInpatientBills() { return validForInpatientBills; }
    public void setValidForInpatientBills(Boolean validForInpatientBills) { this.validForInpatientBills = validForInpatientBills; }

    public Boolean getValidForChanneling() { return validForChanneling; }
    public void setValidForChanneling(Boolean validForChanneling) { this.validForChanneling = validForChanneling; }

    public Boolean getStaffMemberRequired() { return staffMemberRequired; }
    public void setStaffMemberRequired(Boolean staffMemberRequired) { this.staffMemberRequired = staffMemberRequired; }

    public Boolean getMembershipRequired() { return membershipRequired; }
    public void setMembershipRequired(Boolean membershipRequired) { this.membershipRequired = membershipRequired; }

    public Boolean getStaffRequired() { return staffRequired; }
    public void setStaffRequired(Boolean staffRequired) { this.staffRequired = staffRequired; }

    public Boolean getStaffOrFamilyRequired() { return staffOrFamilyRequired; }
    public void setStaffOrFamilyRequired(Boolean staffOrFamilyRequired) { this.staffOrFamilyRequired = staffOrFamilyRequired; }

    public Boolean getMemberRequired() { return memberRequired; }
    public void setMemberRequired(Boolean memberRequired) { this.memberRequired = memberRequired; }

    public Boolean getMemberOrFamilyRequired() { return memberOrFamilyRequired; }
    public void setMemberOrFamilyRequired(Boolean memberOrFamilyRequired) { this.memberOrFamilyRequired = memberOrFamilyRequired; }

    public Boolean getSeniorCitizenRequired() { return seniorCitizenRequired; }
    public void setSeniorCitizenRequired(Boolean seniorCitizenRequired) { this.seniorCitizenRequired = seniorCitizenRequired; }

    public Boolean getPregnantMotherRequired() { return pregnantMotherRequired; }
    public void setPregnantMotherRequired(Boolean pregnantMotherRequired) { this.pregnantMotherRequired = pregnantMotherRequired; }

    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
}
