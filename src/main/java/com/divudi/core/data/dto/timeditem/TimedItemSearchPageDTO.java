/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.timeditem;

import java.util.List;

/**
 * One page of timed item search results.
 *
 * <p>Carries {@code total} alongside the rows so a caller can tell whether it is
 * seeing the whole catalogue or only the first slice — without it, a client paging
 * through the list has no way to know when to stop (issue #23236 §4).
 *
 * @author Buddhika
 */
public class TimedItemSearchPageDTO {

    private List<TimedItemSearchResultDTO> items;
    /** Rows matching the filters, ignoring limit/offset. */
    private long total;
    private int limit;
    private int offset;

    public TimedItemSearchPageDTO() {
    }

    public TimedItemSearchPageDTO(List<TimedItemSearchResultDTO> items, long total, int limit, int offset) {
        this.items = items;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
    }

    public List<TimedItemSearchResultDTO> getItems() {
        return items;
    }

    public void setItems(List<TimedItemSearchResultDTO> items) {
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
