package com.divudi.core.data.dto.forms;

import java.util.Date;

public class FormEntryDto {
    private Long id;
    private Long formTemplateId;
    private String formTemplateName;
    private String comments;
    private Date createdAt;
    private String createdBy;

    public FormEntryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFormTemplateId() { return formTemplateId; }
    public void setFormTemplateId(Long formTemplateId) { this.formTemplateId = formTemplateId; }
    public String getFormTemplateName() { return formTemplateName; }
    public void setFormTemplateName(String formTemplateName) { this.formTemplateName = formTemplateName; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
