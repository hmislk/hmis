package com.divudi.core.data.dto.investigation;

public class TestFlagDTO {

    private Long id;
    private Long investigationItemId;
    private String investigationItemName;
    private Long investigationItemOfValueTypeId;
    private String valueItemName;
    private Long investigationItemOfFlagTypeId;
    private String flagItemName;
    private String sex;
    private Long fromAge;
    private Long toAge;
    private Double fromVal;
    private Double toVal;
    private String flagMessage;
    private String highMessage;
    private String lowMessage;
    private String normalMessage;
    private Boolean displayFlagMessage;
    private Boolean displayHighMessage;
    private Boolean displayLowMessage;
    private Boolean displayNormalMessage;
    private String message;

    public TestFlagDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInvestigationItemId() { return investigationItemId; }
    public void setInvestigationItemId(Long investigationItemId) { this.investigationItemId = investigationItemId; }
    public String getInvestigationItemName() { return investigationItemName; }
    public void setInvestigationItemName(String investigationItemName) { this.investigationItemName = investigationItemName; }
    public Long getInvestigationItemOfValueTypeId() { return investigationItemOfValueTypeId; }
    public void setInvestigationItemOfValueTypeId(Long investigationItemOfValueTypeId) { this.investigationItemOfValueTypeId = investigationItemOfValueTypeId; }
    public String getValueItemName() { return valueItemName; }
    public void setValueItemName(String valueItemName) { this.valueItemName = valueItemName; }
    public Long getInvestigationItemOfFlagTypeId() { return investigationItemOfFlagTypeId; }
    public void setInvestigationItemOfFlagTypeId(Long investigationItemOfFlagTypeId) { this.investigationItemOfFlagTypeId = investigationItemOfFlagTypeId; }
    public String getFlagItemName() { return flagItemName; }
    public void setFlagItemName(String flagItemName) { this.flagItemName = flagItemName; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public Long getFromAge() { return fromAge; }
    public void setFromAge(Long fromAge) { this.fromAge = fromAge; }
    public Long getToAge() { return toAge; }
    public void setToAge(Long toAge) { this.toAge = toAge; }
    public Double getFromVal() { return fromVal; }
    public void setFromVal(Double fromVal) { this.fromVal = fromVal; }
    public Double getToVal() { return toVal; }
    public void setToVal(Double toVal) { this.toVal = toVal; }
    public String getFlagMessage() { return flagMessage; }
    public void setFlagMessage(String flagMessage) { this.flagMessage = flagMessage; }
    public String getHighMessage() { return highMessage; }
    public void setHighMessage(String highMessage) { this.highMessage = highMessage; }
    public String getLowMessage() { return lowMessage; }
    public void setLowMessage(String lowMessage) { this.lowMessage = lowMessage; }
    public String getNormalMessage() { return normalMessage; }
    public void setNormalMessage(String normalMessage) { this.normalMessage = normalMessage; }
    public Boolean getDisplayFlagMessage() { return displayFlagMessage; }
    public void setDisplayFlagMessage(Boolean displayFlagMessage) { this.displayFlagMessage = displayFlagMessage; }
    public Boolean getDisplayHighMessage() { return displayHighMessage; }
    public void setDisplayHighMessage(Boolean displayHighMessage) { this.displayHighMessage = displayHighMessage; }
    public Boolean getDisplayLowMessage() { return displayLowMessage; }
    public void setDisplayLowMessage(Boolean displayLowMessage) { this.displayLowMessage = displayLowMessage; }
    public Boolean getDisplayNormalMessage() { return displayNormalMessage; }
    public void setDisplayNormalMessage(Boolean displayNormalMessage) { this.displayNormalMessage = displayNormalMessage; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
