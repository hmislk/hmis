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
        assertTrue(g.tryAcquire(1L, claimRequest));
        assertFalse(g.tryAcquire(1L, cancelRequest));
    }

    @Test
    public void sameRequestCanTakeItsBillAgain() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest));
        assertTrue(g.tryAcquire(1L, claimRequest));
    }

    @Test
    public void otherBillsAreNotBlocked() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest));
        assertTrue(g.tryAcquire(2L, cancelRequest));
    }

    @Test
    public void releasedBillCanBeTaken() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest));
        g.release(1L, claimRequest);
        assertTrue(g.tryAcquire(1L, cancelRequest));
    }

    @Test
    public void releaseByAnotherRequestDoesNothing() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest));
        g.release(1L, cancelRequest);
        assertFalse(g.tryAcquire(1L, cancelRequest));
    }

    @Test
    public void holdNeverExpiresWhileHeld() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(1L, claimRequest));
        for (int i = 0; i < 1000; i++) {
            assertFalse(g.tryAcquire(1L, cancelRequest));
        }
        g.release(1L, claimRequest);
        assertTrue(g.tryAcquire(1L, cancelRequest));
    }

    @Test
    public void nullsAreIgnored() {
        ClaimBillGuard g = new ClaimBillGuard();
        assertTrue(g.tryAcquire(null, claimRequest));
        assertTrue(g.tryAcquire(1L, null));
        g.release(null, claimRequest);
    }
}
