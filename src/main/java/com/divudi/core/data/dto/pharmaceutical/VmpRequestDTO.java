package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Request DTO for VMP (Virtual Medicinal Product) create/update operations.
 * Extends base with vtmId and dosageFormId relationships.
 *
 * @author Buddhika
 */
public class VmpRequestDTO extends PharmaceuticalItemBaseRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long vtmId;
    private Long dosageFormId;

    public VmpRequestDTO() {
    }

    public Long getVtmId() {
        return vtmId;
    }

    public void setVtmId(Long vtmId) {
        this.vtmId = vtmId;
    }

    public Long getDosageFormId() {
        return dosageFormId;
    }

    public void setDosageFormId(Long dosageFormId) {
        this.dosageFormId = dosageFormId;
    }
}
