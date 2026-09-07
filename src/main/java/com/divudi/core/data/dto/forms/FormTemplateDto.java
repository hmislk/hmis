package com.divudi.core.data.dto.forms;

public class FormTemplateDto {
    private Long id;
    private String name;
    private String description;
    private String formCssClass;
    private int fieldCount;

    public FormTemplateDto() {}

    public FormTemplateDto(Long id, String name, String description, String formCssClass, int fieldCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.formCssClass = formCssClass;
        this.fieldCount = fieldCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFormCssClass() { return formCssClass; }
    public void setFormCssClass(String formCssClass) { this.formCssClass = formCssClass; }
    public int getFieldCount() { return fieldCount; }
    public void setFieldCount(int fieldCount) { this.fieldCount = fieldCount; }
}
