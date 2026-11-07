package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;

/**
 * Result of a single item-supplier link creation within a bulk request.
 */
public class BulkItemSupplierResultDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long supplierId;
    private Long itemId;
    private String outcome; // "created", "already_exists", "error"
    private Integer code;    // 201, 409, 400, 404
    private Long id;        // link id if created or already exists
    private String message;
    private ItemSupplierDto data;

    public BulkItemSupplierResultDto() {
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ItemSupplierDto getData() {
        return data;
    }

    public void setData(ItemSupplierDto data) {
        this.data = data;
    }
}
