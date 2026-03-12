package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Request DTO for category-based config entities (PharmaceuticalItemCategory, DosageForm).
 *
 * @author Buddhika
 */
public class CategoryConfigRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String code;
    private String description;

    public CategoryConfigRequestDTO() {
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
}
