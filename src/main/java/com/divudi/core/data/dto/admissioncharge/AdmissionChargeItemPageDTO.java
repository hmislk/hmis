/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.admissioncharge;

import java.util.List;

/**
 * One page of {@code AdmissionChargeItem} search results (issue #23594).
 *
 * <p>Carries {@code total} alongside the rows so a caller can tell whether it
 * is seeing the whole configuration set or only the first slice.</p>
 *
 * @author Buddhika
 */
public class AdmissionChargeItemPageDTO {

    private List<AdmissionChargeItemDTO> items;
    /** Rows matching the filters, ignoring limit/offset. */
    private long total;
    private int limit;
    private int offset;

    public AdmissionChargeItemPageDTO() {
    }

    public AdmissionChargeItemPageDTO(List<AdmissionChargeItemDTO> items, long total, int limit, int offset) {
        this.items = items;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
    }

    public List<AdmissionChargeItemDTO> getItems() {
        return items;
    }

    public void setItems(List<AdmissionChargeItemDTO> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
