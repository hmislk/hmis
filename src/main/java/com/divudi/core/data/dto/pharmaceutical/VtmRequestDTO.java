package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Request DTO for VTM (Virtual Therapeutic Moiety) create/update operations.
 * Extends base with instructions field.
 *
 * @author Buddhika
 */
public class VtmRequestDTO extends PharmaceuticalItemBaseRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String instructions;

    public VtmRequestDTO() {
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
