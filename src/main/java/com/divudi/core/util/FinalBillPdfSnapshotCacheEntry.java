package com.divudi.core.util;

import java.io.Serializable;

/**
 * Immutable pairing of a Bill's id with the frozen PDF snapshot bytes
 * generated for it, used as a single session-cache field by
 * {@code InwardSearch}/{@code BhtSummeryController} instead of two separate
 * fields (bill id, bytes).
 *
 * <p>The two-separate-field version allowed a real (if narrow) race: two
 * concurrent requests in the same {@code @SessionScoped} bean, refreshing
 * the cache for two different bills, could interleave their writes to the
 * two fields independently (e.g. request A writes bytes-for-A, request B
 * writes bytes-for-B, then B writes id-for-B, then A writes id-for-A) ending
 * in a bytes/id pair that never corresponds to any single generation call —
 * bill B's bytes served under bill A's id, passing the id-match guard for a
 * reader who has bill A loaded. Replacing both fields with one holder,
 * always constructed in a single expression and assigned to the field in
 * one statement, makes that combination atomic: any read of the field sees
 * either a fully-old or a fully-new (billId, bytes) pair, never a mix of
 * the two.
 */
public final class FinalBillPdfSnapshotCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long billId;
    private final byte[] bytes;

    public FinalBillPdfSnapshotCacheEntry(Long billId, byte[] bytes) {
        this.billId = billId;
        this.bytes = bytes;
    }

    public Long getBillId() {
        return billId;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
