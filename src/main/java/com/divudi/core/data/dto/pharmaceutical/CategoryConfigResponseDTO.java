package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Response DTO for category-based config entities (PharmaceuticalItemCategory, DosageForm).
 *
 * @author Buddhika
 */
public class CategoryConfigResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String description;
    private Boolean retired;

    public CategoryConfigResponseDTO() {
    }

    public CategoryConfigResponseDTO(Long id, String name, String code, String description, Boolean retired) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.retired = retired;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }
}
