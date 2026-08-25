package com.divudi.core.data.dto.adjustment;

public class BackfillRequestDTO {
    private Long departmentId;
    private String fromDate;
    private String toDate;
    private boolean apply;

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public boolean isApply() { return apply; }
    public void setApply(boolean apply) { this.apply = apply; }
}
