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

    @Test
    void floorThenPartialCancelRestoresSharesFromTheOriginals() {
        double[] originals = {3000.0, 1000.0};
        // reduction 3500 floors the last share at 0 and spills 2500 to the earlier one
        assertArrayEquals(new double[]{500.0, 0.0},
                CompanyShareAllocation.lowerShares(originals, 3500.0), EPS);
        // a partial cancel makes the reduction fall to 800: recount from the originals
        assertArrayEquals(new double[]{3000.0, 200.0},
                CompanyShareAllocation.lowerShares(originals, 800.0), EPS);
        // the originals are never changed by a recount
        assertArrayEquals(new double[]{3000.0, 1000.0}, originals, EPS);
    }

    @Test
    void companyPaidInFullKeepsItsShare() {
        assertArrayEquals(new double[]{5000.0},
                CompanyShareAllocation.lowerShares(new double[]{5000.0}, new double[]{5000.0}, 1000.0), EPS);
    }

    @Test
    void shareFloorsAtWhatTheCompanyPaid() {
        assertArrayEquals(new double[]{2000.0},
                CompanyShareAllocation.lowerShares(new double[]{5000.0}, new double[]{2000.0}, 4000.0), EPS);
    }

    @Test
    void reductionSkipsAFullyPaidLastCompanyAndGoesToTheEarlierOne() {
        assertArrayEquals(new double[]{2000.0, 1000.0},
                CompanyShareAllocation.lowerShares(new double[]{3000.0, 1000.0},
                        new double[]{0.0, 1000.0}, 1000.0), EPS);
    }

    @Test
    void smallerReductionAfterACancelRestoresFromTheOriginals() {
        double[] originals = {5000.0};
        double[] paid = {2000.0};
        assertArrayEquals(new double[]{2000.0},
                CompanyShareAllocation.lowerShares(originals, paid, 4000.0), EPS);
        assertArrayEquals(new double[]{4000.0},
                CompanyShareAllocation.lowerShares(originals, paid, 1000.0), EPS);
    }

    @Test
    void paidAboveTheShareIsCappedAtTheShare() {
        assertArrayEquals(new double[]{1000.0},
                CompanyShareAllocation.lowerShares(new double[]{1000.0}, new double[]{9000.0}, 500.0), EPS);
    }

    @Test
    void duplicateCommitmentsOfOneCompanyShareOneFloor() {
        // Company 1 holds two commitments (600 + 400) and has paid 700 once; reduction 1000.
        double[] r = CompanyShareAllocation.lowerSharesByCompany(
                new double[]{600.0, 400.0}, new Object[]{1L, 1L}, new double[]{700.0, 700.0}, 1000.0);
        // Company share cannot go below 700: lowered total 700, last commitment lowered first.
        assertArrayEquals(new double[]{600.0, 100.0}, r, EPS);
    }

    @Test
    void floorAppliedPerCompanyNotPerDuplicate() {
        // Without the merge each duplicate would keep min(paid, share) = 600 + 400 = 1000 > paid 700.
        double[] r = CompanyShareAllocation.lowerSharesByCompany(
                new double[]{600.0, 400.0}, new Object[]{1L, 1L}, new double[]{700.0, 700.0}, 0.0);
        assertArrayEquals(new double[]{600.0, 400.0}, r, EPS);
    }

    @Test
    void differentCompaniesStillLowerLastFirst() {
        double[] r = CompanyShareAllocation.lowerSharesByCompany(
                new double[]{3000.0, 2000.0}, new Object[]{1L, 2L}, new double[]{0.0, 500.0}, 2700.0);
        // last company floors at 500 (takes 1500), the first takes the other 1200
        assertArrayEquals(new double[]{1800.0, 500.0}, r, EPS);
    }

    @Test
    void nullKeysAreSeparateCompanies() {
        double[] r = CompanyShareAllocation.lowerSharesByCompany(
                new double[]{100.0, 100.0}, new Object[]{null, null}, null, 150.0);
        assertArrayEquals(new double[]{50.0, 0.0}, r, EPS);
    }

    @Test
    void paidIsSplitOverDuplicatesOnce() {
        double[] f = CompanyShareAllocation.paidFractions(new Object[]{1L, 1L},
                new double[]{600.0, 100.0}, new double[]{700.0, 700.0});
        assertArrayEquals(new double[]{600.0 / 700.0, 100.0 / 700.0}, f, EPS);
        assertArrayEquals(new double[]{1.0}, CompanyShareAllocation.paidFractions(new Object[]{1L},
                new double[]{600.0}, new double[]{700.0}), EPS);
    }
}
