package com.divudi.service;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * The bills this request has taken in {@link ClaimBillGuard} (#25873). They
 * are all freed when the request ends, after its saves have committed.
 */
@RequestScoped
public class ClaimBillRequestGuard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClaimBillGuard guard;

    private final Set<Long> held = new LinkedHashSet<>();

    /** Takes the bill for this request; false when another request holds it. */
    public boolean acquire(Long billId) {
        if (billId == null) {
            return true;
        }
        if (held.contains(billId)) {
            return true;
        }
        if (!guard.tryAcquire(billId, this)) {
            return false;
        }
        held.add(billId);
        return true;
    }

    @PreDestroy
    void releaseAll() {
        for (Long id : held) {
            guard.release(id, this);
        }
        held.clear();
    }
}
