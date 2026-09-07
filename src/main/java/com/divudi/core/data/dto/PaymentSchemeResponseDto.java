package com.divudi.core.data.dto;

public class PaymentSchemeResponseDto {

    private Long id;
    private String name;
    private String printingName;
    private boolean validForPharmacy;
    private boolean validForBilledBills;
    private boolean validForInpatientBills;
    private boolean validForChanneling;
    private boolean staffMemberRequired;
    private boolean membershipRequired;
    private boolean staffRequired;
    private boolean staffOrFamilyRequired;
    private boolean memberRequired;
    private boolean memberOrFamilyRequired;
    private boolean seniorCitizenRequired;
    private boolean pregnantMotherRequired;
    private boolean retired;
    private int orderNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPrintingName() { return printingName; }
    public void setPrintingName(String printingName) { this.printingName = printingName; }

    public boolean isValidForPharmacy() { return validForPharmacy; }
    public void setValidForPharmacy(boolean validForPharmacy) { this.validForPharmacy = validForPharmacy; }

    public boolean isValidForBilledBills() { return validForBilledBills; }
    public void setValidForBilledBills(boolean validForBilledBills) { this.validForBilledBills = validForBilledBills; }

    public boolean isValidForInpatientBills() { return validForInpatientBills; }
    public void setValidForInpatientBills(boolean validForInpatientBills) { this.validForInpatientBills = validForInpatientBills; }

    public boolean isValidForChanneling() { return validForChanneling; }
    public void setValidForChanneling(boolean validForChanneling) { this.validForChanneling = validForChanneling; }

    public boolean isStaffMemberRequired() { return staffMemberRequired; }
    public void setStaffMemberRequired(boolean staffMemberRequired) { this.staffMemberRequired = staffMemberRequired; }

    public boolean isMembershipRequired() { return membershipRequired; }
    public void setMembershipRequired(boolean membershipRequired) { this.membershipRequired = membershipRequired; }

    public boolean isStaffRequired() { return staffRequired; }
    public void setStaffRequired(boolean staffRequired) { this.staffRequired = staffRequired; }

    public boolean isStaffOrFamilyRequired() { return staffOrFamilyRequired; }
    public void setStaffOrFamilyRequired(boolean staffOrFamilyRequired) { this.staffOrFamilyRequired = staffOrFamilyRequired; }

    public boolean isMemberRequired() { return memberRequired; }
    public void setMemberRequired(boolean memberRequired) { this.memberRequired = memberRequired; }

    public boolean isMemberOrFamilyRequired() { return memberOrFamilyRequired; }
    public void setMemberOrFamilyRequired(boolean memberOrFamilyRequired) { this.memberOrFamilyRequired = memberOrFamilyRequired; }

    public boolean isSeniorCitizenRequired() { return seniorCitizenRequired; }
    public void setSeniorCitizenRequired(boolean seniorCitizenRequired) { this.seniorCitizenRequired = seniorCitizenRequired; }

    public boolean isPregnantMotherRequired() { return pregnantMotherRequired; }
    public void setPregnantMotherRequired(boolean pregnantMotherRequired) { this.pregnantMotherRequired = pregnantMotherRequired; }

    public boolean isRetired() { return retired; }
    public void setRetired(boolean retired) { this.retired = retired; }

    public int getOrderNo() { return orderNo; }
    public void setOrderNo(int orderNo) { this.orderNo = orderNo; }
}
