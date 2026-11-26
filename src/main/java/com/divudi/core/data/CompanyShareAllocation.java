package com.divudi.core.data;

/**
 * How a post final patient payment lowers the credit companies' shares of an
 * inpatient final bill (#26159). A pure function, so the rule lives in one
 * place.
 *
 * Rule for now: the company created last is lowered first, then the one
 * before it, and a share never goes below 0.
 */
public final class CompanyShareAllocation {

    private CompanyShareAllocation() {
    }

    /**
     * @param originalShares the shares fixed at settle, in creation order (oldest first)
     * @param reduction the post final payments the final bill carries (payments
     * positive, cancels and refunds negative, already summed); zero or negative
     * means no reduction
     * @return the lowered shares, same order and length, each between 0 and its original
     */
    public static double[] lowerShares(double[] originalShares, double reduction) {
        return lowerShares(originalShares, null, reduction);
    }

    /**
     * Same as {@link #lowerShares(double[], double)} but a company's share never
     * goes below what that company has already paid (#26159): each share is
     * lowered to no less than max(0, min(paid, original)). What is left of the
     * reduction is not applied; it is a patient overpayment (refund due).
     *
     * @param paidAmounts what each company has paid (receipts net of cancels), same
     * order as the shares; null or a short array means 0 for the missing ones
     */
    public static double[] lowerShares(double[] originalShares, double[] paidAmounts, double reduction) {
        if (originalShares == null) {
            return new double[0];
        }
        double[] lowered = new double[originalShares.length];
        double remaining = Math.max(0.0, reduction);
        for (int i = originalShares.length - 1; i >= 0; i--) {
            double share = Math.max(0.0, originalShares[i]);
            double paid = (paidAmounts != null && i < paidAmounts.length) ? paidAmounts[i] : 0.0;
            double floor = Math.max(0.0, Math.min(paid, share));
            double take = Math.min(share - floor, remaining);
            lowered[i] = share - take;
            remaining -= take;
        }
        return lowered;
    }
}
