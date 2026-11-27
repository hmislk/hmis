package com.divudi.core.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Groups commitment indexes by company key; a null key is its own group. Ordered by each group's last index. */
    private static List<List<Integer>> groupByCompany(Object[] keys, int n) {
        Map<Object, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            Object k = (keys != null && i < keys.length && keys[i] != null) ? keys[i] : new Object();
            groups.computeIfAbsent(k, x -> new ArrayList<>()).add(i);
        }
        List<List<Integer>> list = new ArrayList<>(groups.values());
        list.sort((a, b) -> Integer.compare(a.get(a.size() - 1), b.get(b.size() - 1)));
        return list;
    }

    /**
     * Same as {@link #lowerShares(double[], double[], double)} for an admission
     * where one company can hold more than one commitment in the same version
     * (#26163): the shares of a company are added, the floor (what the company
     * has paid, counted once) and the reduction are applied to the company, and
     * the lowered company share is spread back over its commitments, the last
     * one lowered first. Companies are lowered in the order of their last
     * commitment. Commitments with a null key are each their own company.
     *
     * @param companyKeys one key per commitment (for example the company id)
     * @param paidAmounts what the company of each commitment has paid in total
     * (the same figure on every commitment of a company)
     */
    public static double[] lowerSharesByCompany(double[] originalShares, Object[] companyKeys,
            double[] paidAmounts, double reduction) {
        if (originalShares == null) {
            return new double[0];
        }
        int n = originalShares.length;
        List<List<Integer>> groups = groupByCompany(companyKeys, n);
        double[] mergedOriginal = new double[groups.size()];
        double[] mergedPaid = new double[groups.size()];
        for (int g = 0; g < groups.size(); g++) {
            for (int i : groups.get(g)) {
                mergedOriginal[g] += Math.max(0.0, originalShares[i]);
                double p = (paidAmounts != null && i < paidAmounts.length) ? paidAmounts[i] : 0.0;
                mergedPaid[g] = Math.max(mergedPaid[g], p);
            }
        }
        double[] loweredGroups = lowerShares(mergedOriginal, mergedPaid, reduction);
        double[] lowered = new double[n];
        for (int g = 0; g < groups.size(); g++) {
            double cut = mergedOriginal[g] - loweredGroups[g];
            List<Integer> members = groups.get(g);
            for (int m = members.size() - 1; m >= 0; m--) {
                int i = members.get(m);
                double share = Math.max(0.0, originalShares[i]);
                double take = Math.min(share, Math.max(0.0, cut));
                lowered[i] = share - take;
                cut -= take;
            }
        }
        return lowered;
    }

    /**
     * What fraction of its company's paid total each commitment carries, so the
     * paid figure is not counted on every duplicate commitment (#26163). The
     * paid total fills the commitments in order up to each lowered share; any
     * excess goes on the last one. A company with one commitment gets 1.
     */
    public static double[] paidFractions(Object[] companyKeys, double[] loweredShares, double[] paidAmounts) {
        int n = loweredShares == null ? 0 : loweredShares.length;
        double[] fractions = new double[n];
        for (List<Integer> members : groupByCompany(companyKeys, n)) {
            double total = 0.0;
            for (int i : members) {
                double p = (paidAmounts != null && i < paidAmounts.length) ? paidAmounts[i] : 0.0;
                total = Math.max(total, p);
            }
            if (members.size() == 1 || total <= 0) {
                for (int i : members) {
                    fractions[i] = 1.0 / members.size();
                }
                continue;
            }
            double remaining = total;
            for (int m = 0; m < members.size(); m++) {
                int i = members.get(m);
                double a = (m == members.size() - 1) ? remaining : Math.min(Math.max(0.0, loweredShares[i]), remaining);
                fractions[i] = a / total;
                remaining -= a;
            }
        }
        return fractions;
    }
}
