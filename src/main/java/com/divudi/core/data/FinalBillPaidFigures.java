package com.divudi.core.data;

import java.io.Serializable;

/**
 * The money figures of an inpatient final bill, recounted from the bills
 * (#26152). One place decides how the parts add up, so every path that changes
 * a final bill's paid amount (patient payment, deposit, refund, credit company
 * receipt, post final payment, and the cancel of each) ends with the same
 * numbers.
 *
 * <ul>
 * <li>paidAmount: every inward payment bill (payments, deposits, refunds and
 * cancels, signed), plus the post final payments the bill carries, plus the
 * credit company receipts net of their cancels.</li>
 * <li>settledAmountByPatient: the same, leaving out the credit method (old
 * flows used it for company money) and the company receipts.</li>
 * <li>settledAmountBySponsor: the credit company receipts net of their
 * cancels.</li>
 * </ul>
 */
public final class FinalBillPaidFigures implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double paidAmount;
    private final double settledAmountByPatient;
    private final double settledAmountBySponsor;

    private FinalBillPaidFigures(double paidAmount, double settledAmountByPatient, double settledAmountBySponsor) {
        this.paidAmount = paidAmount;
        this.settledAmountByPatient = settledAmountByPatient;
        this.settledAmountBySponsor = settledAmountBySponsor;
    }

    /**
     * @param allInwardPayments sum of every inward payment bill, signed
     * @param creditMethodInwardPayments the part of it paid with the Credit
     * method
     * @param postFinalPayments post final payments the final bill carries, signed
     * @param companyReceiptsNet credit company receipts less their cancels
     */
    public static FinalBillPaidFigures of(double allInwardPayments, double creditMethodInwardPayments,
            double postFinalPayments, double companyReceiptsNet) {
        double paid = allInwardPayments + postFinalPayments + companyReceiptsNet;
        double byPatient = allInwardPayments - creditMethodInwardPayments + postFinalPayments;
        return new FinalBillPaidFigures(paid, byPatient, companyReceiptsNet);
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public double getSettledAmountByPatient() {
        return settledAmountByPatient;
    }

    public double getSettledAmountBySponsor() {
        return settledAmountBySponsor;
    }

    /** The balance of a final bill with this net total. */
    public double balanceFor(double netTotal) {
        return netTotal - paidAmount;
    }
}
