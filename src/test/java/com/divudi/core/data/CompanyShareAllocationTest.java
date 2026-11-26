package com.divudi.core.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class CompanyShareAllocationTest {

    private static final double EPS = 0.0001;

    @Test
    void oneCompanyLosesTheReduction() {
        assertArrayEquals(new double[]{4722.0},
                CompanyShareAllocation.lowerShares(new double[]{5722.0}, 1000.0), EPS);
    }

    @Test
    void lastCompanyIsLoweredFirstThenTheOneBefore() {
        assertArrayEquals(new double[]{3000.0, 0.0},
                CompanyShareAllocation.lowerShares(new double[]{3000.0, 1000.0}, 1000.0), EPS);
        assertArrayEquals(new double[]{2500.0, 0.0},
                CompanyShareAllocation.lowerShares(new double[]{3000.0, 1000.0}, 1500.0), EPS);
    }

    @Test
    void reductionLargerThanAllSharesFloorsAtZero() {
        assertArrayEquals(new double[]{0.0, 0.0},
                CompanyShareAllocation.lowerShares(new double[]{3000.0, 1000.0}, 9000.0), EPS);
    }

    @Test
    void negativeReductionAfterCancelsChangesNothing() {
        assertArrayEquals(new double[]{3000.0, 1000.0},
                CompanyShareAllocation.lowerShares(new double[]{3000.0, 1000.0}, -200.0), EPS);
        assertArrayEquals(new double[]{3000.0, 1000.0},
                CompanyShareAllocation.lowerShares(new double[]{3000.0, 1000.0}, 0.0), EPS);
    }

    @Test
    void noSharesGivesNoShares() {
        assertArrayEquals(new double[0], CompanyShareAllocation.lowerShares(new double[0], 500.0), EPS);
        assertArrayEquals(new double[0], CompanyShareAllocation.lowerShares(null, 500.0), EPS);
    }
}
