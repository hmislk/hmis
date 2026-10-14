package com.divudi.core.data.dto.investigation;

/**
 * A lab report format (the {@code ReportFormat} Category subclass) that owns a
 * common report template — the block of {@code CommonReportItem} rows printed on
 * every report produced in that format.
 */
public class ReportFormatDTO {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer orderNo;
    private Long parentCategoryId;
    private String parentCategoryName;
    private Integer itemCount;
    private String message;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public Long getParentCategoryId() { return parentCategoryId; }
    public void setParentCategoryId(Long parentCategoryId) { this.parentCategoryId = parentCategoryId; }
    public String getParentCategoryName() { return parentCategoryName; }
    public void setParentCategoryName(String parentCategoryName) { this.parentCategoryName = parentCategoryName; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
