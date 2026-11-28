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
 * hospital runs. A hold never expires while its request runs: the request
 * scope always ends, error or not, and frees it; a server restart clears all.
 */
@ApplicationScoped
public class ClaimBillGuard {

    private final Map<Long, Object> holds = new ConcurrentHashMap<>();

    /**
     * Takes the bill for the owner, or returns true if the owner already has it.
     * False when another request holds it.
     */
    public boolean tryAcquire(Long billId, Object owner) {
        if (billId == null || owner == null) {
            return true;
        }
        Object current = holds.putIfAbsent(billId, owner);
        return current == null || current == owner;
    }

    /** Frees the bill if the owner holds it. */
    public void release(Long billId, Object owner) {
        if (billId == null || owner == null) {
            return;
        }
        holds.remove(billId, owner);
    }
}
