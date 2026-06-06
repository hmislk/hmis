package com.divudi.core.data.dto.forms;

public class FormChoiceDto {
    private Long id;
    private String label;
    private String value;
    private Integer orderNo;

    public FormChoiceDto() {}

    public FormChoiceDto(Long id, String label, String value, Integer orderNo) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.orderNo = orderNo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
}
