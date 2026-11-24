package com.divudi.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;

/**
 * Keeps Create Claim and a refund, return or cancel of the same bill from
 * running at the same time (#25873).
 * <p>
 * The refund, return and cancel pages check for an open claim and then save in
 * several transactions, so a database lock cannot cover both. Instead each
 * request takes the bill here before it checks (see
 * {@link ClaimBillRequestGuard}) and keeps it until the request ends. A second
 * request for the same bill is refused at once, never kept waiting.
 * <p>
 * Held in memory, so it covers one application server, which is how each
 * hospital runs.
 */
@ApplicationScoped
public class ClaimBillGuard {

    /** A hold older than this is taken to be left over from a request that never ended. */
    static final long STALE_AFTER_MILLIS = 10L * 60L * 1000L;

    private final Map<Long, Hold> holds = new ConcurrentHashMap<>();

    /**
     * Takes the bill for the owner, or returns true if the owner already has it.
     * False when another request holds it.
     */
    public boolean tryAcquire(Long billId, Object owner) {
        return tryAcquire(billId, owner, System.currentTimeMillis());
    }

    boolean tryAcquire(Long billId, Object owner, long now) {
        if (billId == null || owner == null) {
            return true;
        }
        Hold mine = new Hold(owner, now);
        Hold result = holds.compute(billId, (id, current) -> {
            if (current == null || current.owner == owner || now - current.since > STALE_AFTER_MILLIS) {
                return current != null && current.owner == owner ? current : mine;
            }
            return current;
        });
        return result.owner == owner;
    }

    /** Frees the bill if the owner holds it. */
    public void release(Long billId, Object owner) {
        if (billId == null || owner == null) {
            return;
        }
        holds.computeIfPresent(billId, (id, current) -> current.owner == owner ? null : current);
    }

    private static final class Hold {

        private final Object owner;
        private final long since;

        private Hold(Object owner, long since) {
            this.owner = owner;
            this.since = since;
        }
    }
}
