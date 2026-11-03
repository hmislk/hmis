package com.divudi.core.data.dto.pharmaceutical;

import java.io.Serializable;

/**
 * Request DTO for AMPP (Actual Medicinal Product Package) create/update operations.
 * Extends base with ampId, dblValue, and packUnitId.
 *
 * @author Buddhika
 */
public class AmppRequestDTO extends PharmaceuticalItemBaseRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long ampId;
    private Double dblValue;
    private Long packUnitId;
    private Long vmppId;

    public AmppRequestDTO() {
    }

    public Long getAmpId() {
        return ampId;
    }

    public void setAmpId(Long ampId) {
        this.ampId = ampId;
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

    public Long getVmppId() {
        return vmppId;
    }

    public void setVmppId(Long vmppId) {
        this.vmppId = vmppId;
    }
}
