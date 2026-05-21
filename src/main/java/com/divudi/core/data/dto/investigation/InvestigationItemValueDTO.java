package com.divudi.core.data.dto.investigation;

public class InvestigationItemValueDTO {

    private Long id;
    private String name;
    private String code;
    private Integer orderNo;
    private Long investigationItemId;
    private String investigationItemName;
    private String message;

    public InvestigationItemValueDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public Long getInvestigationItemId() { return investigationItemId; }
    public void setInvestigationItemId(Long investigationItemId) { this.investigationItemId = investigationItemId; }
    public String getInvestigationItemName() { return investigationItemName; }
    public void setInvestigationItemName(String investigationItemName) { this.investigationItemName = investigationItemName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
