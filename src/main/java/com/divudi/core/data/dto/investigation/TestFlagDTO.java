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
    private long fromAge;
    private long toAge;
    private double fromVal;
    private double toVal;
    private String flagMessage;
    private String highMessage;
    private String lowMessage;
    private String normalMessage;
    private boolean displayFlagMessage;
    private boolean displayHighMessage;
    private boolean displayLowMessage;
    private boolean displayNormalMessage;
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
    public long getFromAge() { return fromAge; }
    public void setFromAge(long fromAge) { this.fromAge = fromAge; }
    public long getToAge() { return toAge; }
    public void setToAge(long toAge) { this.toAge = toAge; }
    public double getFromVal() { return fromVal; }
    public void setFromVal(double fromVal) { this.fromVal = fromVal; }
    public double getToVal() { return toVal; }
    public void setToVal(double toVal) { this.toVal = toVal; }
    public String getFlagMessage() { return flagMessage; }
    public void setFlagMessage(String flagMessage) { this.flagMessage = flagMessage; }
    public String getHighMessage() { return highMessage; }
    public void setHighMessage(String highMessage) { this.highMessage = highMessage; }
    public String getLowMessage() { return lowMessage; }
    public void setLowMessage(String lowMessage) { this.lowMessage = lowMessage; }
    public String getNormalMessage() { return normalMessage; }
    public void setNormalMessage(String normalMessage) { this.normalMessage = normalMessage; }
    public boolean isDisplayFlagMessage() { return displayFlagMessage; }
    public void setDisplayFlagMessage(boolean displayFlagMessage) { this.displayFlagMessage = displayFlagMessage; }
    public boolean isDisplayHighMessage() { return displayHighMessage; }
    public void setDisplayHighMessage(boolean displayHighMessage) { this.displayHighMessage = displayHighMessage; }
    public boolean isDisplayLowMessage() { return displayLowMessage; }
    public void setDisplayLowMessage(boolean displayLowMessage) { this.displayLowMessage = displayLowMessage; }
    public boolean isDisplayNormalMessage() { return displayNormalMessage; }
    public void setDisplayNormalMessage(boolean displayNormalMessage) { this.displayNormalMessage = displayNormalMessage; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
