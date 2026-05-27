package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Request DTO for VMPP (Virtual Medicinal Product Pack) create/update operations.
 * Extends base with vmpId, dblValue, and packUnitId.
 *
 * @author Buddhika
 */
public class VmppRequestDTO extends PharmaceuticalItemBaseRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long vmpId;
    private Double dblValue;
    private Long packUnitId;

    public VmppRequestDTO() {
    }

    public Long getVmpId() {
        return vmpId;
    }

    public void setVmpId(Long vmpId) {
        this.vmpId = vmpId;
    }

    public Double getDblValue() {
        return dblValue;
    }

    public void setDblValue(Double dblValue) {
        this.dblValue = dblValue;
    }

    public Long getPackUnitId() {
        return packUnitId;
    }

    public void setPackUnitId(Long packUnitId) {
        this.packUnitId = packUnitId;
    }
}
