package com.divudi.core.data.dto.investigation;

public class DynamicLabelDTO {

    private Long id;
    private Long investigationItemId;
    private String investigationItemName;
    private Long investigationItemOfLabelTypeId;
    private String labelItemName;
    private String sex;
    private long fromAge;
    private long toAge;
    private String flagMessage;
    private String message;

    public DynamicLabelDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInvestigationItemId() { return investigationItemId; }
    public void setInvestigationItemId(Long investigationItemId) { this.investigationItemId = investigationItemId; }
    public String getInvestigationItemName() { return investigationItemName; }
    public void setInvestigationItemName(String investigationItemName) { this.investigationItemName = investigationItemName; }
    public Long getInvestigationItemOfLabelTypeId() { return investigationItemOfLabelTypeId; }
    public void setInvestigationItemOfLabelTypeId(Long investigationItemOfLabelTypeId) { this.investigationItemOfLabelTypeId = investigationItemOfLabelTypeId; }
    public String getLabelItemName() { return labelItemName; }
    public void setLabelItemName(String labelItemName) { this.labelItemName = labelItemName; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public long getFromAge() { return fromAge; }
    public void setFromAge(long fromAge) { this.fromAge = fromAge; }
    public long getToAge() { return toAge; }
    public void setToAge(long toAge) { this.toAge = toAge; }
    public String getFlagMessage() { return flagMessage; }
    public void setFlagMessage(String flagMessage) { this.flagMessage = flagMessage; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
