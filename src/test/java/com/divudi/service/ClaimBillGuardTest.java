package com.divudi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** #25873: Create Claim and a refund, return or cancel of the same bill cannot run at once. */
public class ClaimBillGuardTest {

    private final Object claimRequest = new Object();
    private final Object cancelRequest = new Object();

    @Test
    public void secondRequestForSameBillIsRefused() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest, 1000));
        assertFalse(g.tryAcquire(1L, cancelRequest, 1001));
    }

    @Test
    public void sameRequestCanTakeItsBillAgain() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest, 1000));
        assertTrue(g.tryAcquire(1L, claimRequest, 1001));
    }

    @Test
    public void otherBillsAreNotBlocked() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest, 1000));
        assertTrue(g.tryAcquire(2L, cancelRequest, 1001));
    }

    @Test
    public void releasedBillCanBeTaken() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest, 1000));
        g.release(1L, claimRequest);
        assertTrue(g.tryAcquire(1L, cancelRequest, 1001));
    }

    @Test
    public void releaseByAnotherRequestDoesNothing() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest, 1000));
        g.release(1L, cancelRequest);
        assertFalse(g.tryAcquire(1L, cancelRequest, 1001));
    }

    @Test
    public void staleHoldIsTakenOver() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest, 1000));
        assertFalse(g.tryAcquire(1L, cancelRequest, 1000 + ClaimBillGuard.STALE_AFTER_MILLIS));
        assertTrue(g.tryAcquire(1L, cancelRequest, 1001 + ClaimBillGuard.STALE_AFTER_MILLIS));
        // The old owner's late release must not free the new owner's hold.
        g.release(1L, claimRequest);
        assertFalse(g.tryAcquire(1L, claimRequest, 1002 + ClaimBillGuard.STALE_AFTER_MILLIS));
    }

    @Test
    public void nullsAreIgnored() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(null, claimRequest, 1000));
        assertTrue(g.tryAcquire(1L, null, 1000));
        g.release(null, claimRequest);
    }
}
