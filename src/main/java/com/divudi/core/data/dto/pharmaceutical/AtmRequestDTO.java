package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Request DTO for ATM (Anatomical Therapeutic Material) create/update operations.
 * Extends base with vtmId relationship.
 *
 * @author Buddhika
 */
public class AtmRequestDTO extends PharmaceuticalItemBaseRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long vtmId;

    public AtmRequestDTO() {
    }

    public Long getVtmId() {
        return vtmId;
    }

    public void setVtmId(Long vtmId) {
        this.vtmId = vtmId;
    }
}
