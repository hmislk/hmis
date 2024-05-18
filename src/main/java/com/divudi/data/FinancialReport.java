/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class FinancialReport {

    private AtomicBillTypeTotals atomicBillTypeTotals;

    // Payment types with collected, refunded, and net total
    private double collectedCash;
    private double refundedCash;
    private double netCashTotal;

    private double collectedCredit;
    private double refundedCredit;
    private double netCreditTotal;

    private double collectedCreditCard;
    private double refundedCreditCard;
    private double netCreditCardTotal;

    private double collectedOtherNonCredit;
    private double refundedOtherNonCredit;
    private double netOtherNonCreditTotal;

    private double collectedVoucher;
    private double refundedVoucher;
    private double netVoucherTotal;

    // Totals
    private double cashTotal;

    // Floats and handovers
    private double floatReceived;
    private double floatHandover;
    private double shiftStartFunds;
    private double shiftEndFunds;

    // Other transactions
    private double cashCollectedTransferIn;
    private double bankWithdrawals;
    private double receivedFromCollectionCenter;
    private double totalShiftCashIn;

    private double drPayments;
    private double otherPayments;
    private double cashGivenOutTransferOut;
    private double bankDeposits;
    private double shortExcess;

    // Final total
    private double total;

    // Lists to hold the PaymentMethods and BillTypeAtomics for each variable
    private List<PaymentMethod> paymentMethodsForCollectedCash;
    private List<BillTypeAtomic> billTypesForCollectedCash;

    private List<PaymentMethod> paymentMethodsForRefundedCash;
    private List<BillTypeAtomic> billTypesForRefundedCash;

    private List<PaymentMethod> paymentMethodsForNetCashTotal;
    private List<BillTypeAtomic> billTypesForNetCashTotal;

    private List<PaymentMethod> paymentMethodsForCollectedCredit;
    private List<BillTypeAtomic> billTypesForCollectedCredit;

    private List<PaymentMethod> paymentMethodsForRefundedCredit;
    private List<BillTypeAtomic> billTypesForRefundedCredit;

    private List<PaymentMethod> paymentMethodsForNetCreditTotal;
    private List<BillTypeAtomic> billTypesForNetCreditTotal;

    private List<PaymentMethod> paymentMethodsForCollectedCreditCard;
    private List<BillTypeAtomic> billTypesForCollectedCreditCard;

    private List<PaymentMethod> paymentMethodsForRefundedCreditCard;
    private List<BillTypeAtomic> billTypesForRefundedCreditCard;

    private List<PaymentMethod> paymentMethodsForNetCreditCardTotal;
    private List<BillTypeAtomic> billTypesForNetCreditCardTotal;

    private List<PaymentMethod> paymentMethodsForCollectedDebitCard;
    private List<BillTypeAtomic> billTypesForCollectedDebitCard;

    private List<PaymentMethod> paymentMethodsForRefundedDebitCard;
    private List<BillTypeAtomic> billTypesForRefundedDebitCard;

    private List<PaymentMethod> paymentMethodsForNetDebitCardTotal;
    private List<BillTypeAtomic> billTypesForNetDebitCardTotal;

    private List<PaymentMethod> paymentMethodsForCollectedVoucher;
    private List<BillTypeAtomic> billTypesForCollectedVoucher;

    private List<PaymentMethod> paymentMethodsForRefundedVoucher;
    private List<BillTypeAtomic> billTypesForRefundedVoucher;

    private List<PaymentMethod> paymentMethodsForNetVoucherTotal;
    private List<BillTypeAtomic> billTypesForNetVoucherTotal;

// Lists for floats, handovers, and other transactions...
    private List<PaymentMethod> paymentMethodsForFloatCollected;
    private List<BillTypeAtomic> billTypesForFloatCollected;

    private List<PaymentMethod> paymentMethodsForFloatHandover;
    private List<BillTypeAtomic> billTypesForFloatHandover;

    private List<PaymentMethod> paymentMethodsForFloatMySafe;
    private List<BillTypeAtomic> billTypesForFloatMySafe;

    private List<PaymentMethod> paymentMethodsForShiftEndHandovers;
    private List<BillTypeAtomic> billTypesOfShiftEndHandovers;

// Lists for the rest of the transactions...
    private List<PaymentMethod> paymentMethodsForCashCollectedTransferIn;
    private List<BillTypeAtomic> billTypesForCashCollectedTransferIn;

    private List<PaymentMethod> paymentMethodsForBankWithdrawals;
    private List<BillTypeAtomic> billTypesForBankWithdrawals;

    private List<PaymentMethod> paymentMethodsForReceivedFromCollectionCenter;
    private List<BillTypeAtomic> billTypesForReceivedFromCollectionCenter;

    private List<PaymentMethod> paymentMethodsForTotalShiftCashIn;
    private List<BillTypeAtomic> billTypesForTotalShiftCashIn;

    private List<PaymentMethod> paymentMethodsForDrPayments;
    private List<BillTypeAtomic> billTypesForDrPayments;

    private List<PaymentMethod> paymentMethodsForOtherPayments;
    private List<BillTypeAtomic> billTypesForOtherPayments;

    private List<PaymentMethod> paymentMethodsForCashGivenOutTransferOut;
    private List<BillTypeAtomic> billTypesForCashGivenOutTransferOut;

    private List<PaymentMethod> paymentMethodsForBankDeposits;
    private List<BillTypeAtomic> billTypesForBankDeposits;

    private List<PaymentMethod> paymentMethodsForShortExcess;
    private List<BillTypeAtomic> billTypesForShortExcess;

    public FinancialReport(AtomicBillTypeTotals atomicBillTypeTotals) {
        this.atomicBillTypeTotals = atomicBillTypeTotals;
    }

    public FinancialReport() {
    }

    public void calculateTotal() {
        System.out.println("Starting calculation of totals.");

        // Update and sum up all the transaction types
        netCashTotal = getNetCashTotal();
        netCreditTotal = getNetCreditTotal();
        netCreditCardTotal = getNetCreditCardTotal();
        netOtherNonCreditTotal = getNetOtherNonCreditTotal();
        netVoucherTotal = getNetVoucherTotal();

        shiftStartFunds = getShiftStartFunds();
        floatHandover = getFloatHandover();
        floatReceived = getFloatReceived();
        shiftEndFunds = getShiftEndFunds();

        bankWithdrawals = getBankWithdrawals();
        bankDeposits = getBankDeposits();

        // Calculate the overall total starting from the float my safe value and adjusting with all other totals
        total = shiftStartFunds;  // Start with the float my safe value
        System.out.println("Initial Total (Float My Safe): " + total);

        // Add net totals of all transaction types
        total += netCashTotal;
        System.out.println("Total after adding Net Cash: " + total);

        total += netCreditTotal;
        System.out.println("Total after adding Net Credit: " + total);

        total += netCreditCardTotal;
        System.out.println("Total after adding Net Credit Card: " + total);

        total += netOtherNonCreditTotal;
        System.out.println("Total after adding Net Other Non-Credit: " + total);

        total += netVoucherTotal;
        System.out.println("Total after adding Net Voucher: " + total);

        // Adjust float and handover transactions
        total += floatReceived;
        System.out.println("Total after adding Float Collected: " + total);

        total += floatHandover;
        System.out.println("Total after adding Float Handover: " + total);

        total -= shiftEndFunds;
        System.out.println("Total after deducting Collection Handover: " + total);

        // Adjust bank transactions
        total += bankWithdrawals - bankDeposits;
        System.out.println("Total after adjusting Bank Withdrawals and Deposits: " + total);

        System.out.println("Final Total: " + total);
    }

    public double getCollectedCash() {
        collectedCash = atomicBillTypeTotals.getTotal(getBillTypesForCollectedCash(), getPaymentMethodsForCollectedCash());
        return collectedCash;
    }

    public double getRefundedCash() {
        refundedCash = atomicBillTypeTotals.getTotal(getBillTypesForRefundedCash(), getPaymentMethodsForRefundedCash());
        return refundedCash;
    }

    public double getNetCashTotal() {
        netCashTotal = getCollectedCash() - getRefundedCash();
        return netCashTotal;
    }

    public double getCollectedCredit() {
        collectedCredit = atomicBillTypeTotals.getTotal(getBillTypesForCollectedCredit(), getPaymentMethodsForCollectedCredit());
        return collectedCredit;
    }

    public double getRefundedCredit() {
        refundedCredit = atomicBillTypeTotals.getTotal(getBillTypesForRefundedCredit(), getPaymentMethodsForRefundedCredit());
        return refundedCredit;
    }

    public double getNetCreditTotal() {
        netCreditTotal = getCollectedCredit() - getRefundedCredit();
        return netCreditTotal;
    }

    public double getCollectedCreditCard() {
        collectedCreditCard = atomicBillTypeTotals.getTotal(getBillTypesForCollectedCreditCard(), getPaymentMethodsForCollectedCreditCard());
        return collectedCreditCard;
    }

    public double getRefundedCreditCard() {
        refundedCreditCard = atomicBillTypeTotals.getTotal(getBillTypesForRefundedCreditCard(), getPaymentMethodsForRefundedCreditCard());
        return refundedCreditCard;
    }

    public double getNetCreditCardTotal() {
        netCreditCardTotal = getCollectedCreditCard() - getRefundedCreditCard();
        return netCreditCardTotal;
    }

    public double getCollectedOtherNonCredit() {
        collectedOtherNonCredit = atomicBillTypeTotals.getTotal(getBillTypesForCollectedDebitCard(), getPaymentMethodsForCollectedDebitCard());
        return collectedOtherNonCredit;
    }

    public double getRefundedOtherNonCredit() {
        refundedOtherNonCredit = atomicBillTypeTotals.getTotal(getBillTypesForRefundedDebitCard(), getPaymentMethodsForRefundedDebitCard());
        return refundedOtherNonCredit;
    }

    public double getNetOtherNonCreditTotal() {
        netOtherNonCreditTotal = getCollectedOtherNonCredit() - getRefundedOtherNonCredit();
        return netOtherNonCreditTotal;
    }

    public double getCollectedVoucher() {
        collectedVoucher = atomicBillTypeTotals.getTotal(getBillTypesForCollectedVoucher(), getPaymentMethodsForCollectedVoucher());
        return collectedVoucher;
    }

    public double getRefundedVoucher() {
        refundedVoucher = atomicBillTypeTotals.getTotal(getBillTypesForRefundedVoucher(), getPaymentMethodsForRefundedVoucher());
        return refundedVoucher;
    }

    public double getNetVoucherTotal() {
        netVoucherTotal = getCollectedVoucher() - getRefundedVoucher();
        return netVoucherTotal;
    }

    public double getCashTotal() {
        cashTotal = getNetCashTotal() + getNetCreditTotal() + getNetCreditCardTotal() + getNetOtherNonCreditTotal() + getNetVoucherTotal();
        return cashTotal;
    }

    public double getFloatReceived() {
        floatReceived = atomicBillTypeTotals.getTotal(getBillTypesForFloatCollected());
        return floatReceived;
    }

    public double getFloatHandover() {
        floatHandover = atomicBillTypeTotals.getTotal(getBillTypesForFloatHandover());
        return floatHandover;
    }

    public double getShiftStartFunds() {
        shiftStartFunds = atomicBillTypeTotals.getTotal(getBillTypesForFloatMySafe());
        return shiftStartFunds;
    }

    public double getShiftEndFunds() {
        shiftEndFunds = atomicBillTypeTotals.getTotal(getBillTypesOfShiftEndHandovers());
        return shiftEndFunds;
    }

    public double getCashCollectedTransferIn() {
        cashCollectedTransferIn = atomicBillTypeTotals.getTotal(getBillTypesForCashCollectedTransferIn());
        return cashCollectedTransferIn;
    }

    public double getBankWithdrawals() {
        bankWithdrawals = atomicBillTypeTotals.getTotal(getBillTypesForBankWithdrawals());
        return bankWithdrawals;
    }

    public double getReceivedFromCollectionCenter() {
        receivedFromCollectionCenter = atomicBillTypeTotals.getTotal(getBillTypesForReceivedFromCollectionCenter(), getPaymentMethodsForReceivedFromCollectionCenter());
        return receivedFromCollectionCenter;
    }

    public double getTotalShiftCashIn() {
        totalShiftCashIn = getFloatReceived() + getCashCollectedTransferIn() + getBankWithdrawals() + getReceivedFromCollectionCenter();
        return totalShiftCashIn;
    }

    public double getDrPayments() {
        drPayments = atomicBillTypeTotals.getTotal(getBillTypesForDrPayments(), getPaymentMethodsForDrPayments());
        return drPayments;
    }

    public double getOtherPayments() {
        otherPayments = atomicBillTypeTotals.getTotal(getBillTypesForOtherPayments(), getPaymentMethodsForOtherPayments());
        return otherPayments;
    }

    public double getCashGivenOutTransferOut() {
        cashGivenOutTransferOut = atomicBillTypeTotals.getTotal(getBillTypesForCashGivenOutTransferOut());
        return cashGivenOutTransferOut;
    }

    public double getBankDeposits() {
        bankDeposits = atomicBillTypeTotals.getTotal(getBillTypesForBankDeposits());
        return bankDeposits;
    }

    public double getShortExcess() {
        shortExcess = atomicBillTypeTotals.getTotal(getBillTypesForShortExcess());
        return shortExcess;
    }

    public double getTotal() {
        calculateTotal();
//        total = getTotalShiftCashIn() - getFloatHandover() - getFloatMySafe() - getCollectionHandover() - getDrPayments() - getOtherPayments() + getCashGivenOutTransferOut() + getBankDeposits() + getShortExcess();
        return total;
    }

    public List<PaymentMethod> getPaymentMethodsForCollectedCash() {
        if (paymentMethodsForCollectedCash == null) {
            paymentMethodsForCollectedCash = new ArrayList<>();
            paymentMethodsForCollectedCash.add(PaymentMethod.Cash); // Add other payment methods if necessary
        }
        return paymentMethodsForCollectedCash;
    }

    public List<BillTypeAtomic> getBillTypesForCollectedCash() {
        if (billTypesForCollectedCash == null) {
            billTypesForCollectedCash = new ArrayList<>();
            billTypesForCollectedCash = BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN);
        }
        return billTypesForCollectedCash;
    }

    public List<PaymentMethod> getPaymentMethodsForRefundedCash() {
        if (paymentMethodsForRefundedCash == null) {
            paymentMethodsForRefundedCash = new ArrayList<>();
            paymentMethodsForRefundedCash.add(PaymentMethod.Cash); // Add other payment methods if necessary
        }
        return paymentMethodsForRefundedCash;
    }

    public List<BillTypeAtomic> getBillTypesForRefundedCash() {
        if (billTypesForRefundedCash == null) {
            billTypesForRefundedCash = new ArrayList<>();
            billTypesForRefundedCash.addAll(BillTypeAtomic.findByCategory(BillCategory.REFUND));
            billTypesForRefundedCash.addAll(BillTypeAtomic.findByCategory(BillCategory.CANCELLATION));
            
        }
        return billTypesForRefundedCash;
    }

    public List<PaymentMethod> getPaymentMethodsForNetCashTotal() {
        if (paymentMethodsForNetCashTotal == null) {
            paymentMethodsForNetCashTotal = new ArrayList<>();
            paymentMethodsForNetCashTotal.addAll(getPaymentMethodsForCollectedCash());
            paymentMethodsForNetCashTotal.addAll(getPaymentMethodsForRefundedCash());
        }
        return paymentMethodsForNetCashTotal;
    }

    public List<BillTypeAtomic> getBillTypesForNetCashTotal() {
        if (billTypesForNetCashTotal == null) {
            billTypesForNetCashTotal = new ArrayList<>();
            billTypesForNetCashTotal.addAll(getBillTypesForCollectedCash());
            billTypesForNetCashTotal.addAll(getBillTypesForRefundedCash());
        }
        return billTypesForNetCashTotal;
    }

    public List<PaymentMethod> getPaymentMethodsForCollectedCredit() {
        if (paymentMethodsForCollectedCredit == null) {
            paymentMethodsForCollectedCredit = new ArrayList<>();
            paymentMethodsForCollectedCredit.add(PaymentMethod.Credit); // Assuming Credit represents collected credit
            // Add any other payment methods if necessary
        }
        return paymentMethodsForCollectedCredit;
    }

    public List<BillTypeAtomic> getBillTypesForCollectedCredit() {
        if (billTypesForCollectedCredit == null) {
            billTypesForCollectedCredit = new ArrayList<>();
            // Add BillTypeAtomic entries that represent credit collections
            // For example, assume CHANNEL_BOOKING_WITH_PAYMENT is collected on credit
            billTypesForCollectedCredit.add(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
            // Add other BillTypeAtomic entries if they are collected as credit
        }
        return billTypesForCollectedCredit;
    }

    public List<PaymentMethod> getPaymentMethodsForRefundedCredit() {
        if (paymentMethodsForRefundedCredit == null) {
            paymentMethodsForRefundedCredit = new ArrayList<>();
            paymentMethodsForRefundedCredit.add(PaymentMethod.Credit); // Assuming Credit also represents refunded credit
            // Add any other payment methods for refunded credit if necessary
        }
        return paymentMethodsForRefundedCredit;
    }

    public List<BillTypeAtomic> getBillTypesForRefundedCredit() {
        if (billTypesForRefundedCredit == null) {
            billTypesForRefundedCredit = new ArrayList<>();
            // Add BillTypeAtomic entries related to credit refunds
            // For example, assume CHANNEL_REFUND is a refund on credit
            billTypesForRefundedCredit.add(BillTypeAtomic.CHANNEL_REFUND);
            // Add other BillTypeAtomic entries if they represent refunds on credit
        }
        return billTypesForRefundedCredit;
    }

    public List<PaymentMethod> getPaymentMethodsForNetCreditTotal() {
        if (paymentMethodsForNetCreditTotal == null) {
            paymentMethodsForNetCreditTotal = new ArrayList<>();
            paymentMethodsForNetCreditTotal.addAll(getPaymentMethodsForCollectedCredit());
            paymentMethodsForNetCreditTotal.addAll(getPaymentMethodsForRefundedCredit());
            // Remove duplicates if needed, since it's a net total we might only need the unique methods
        }
        return paymentMethodsForNetCreditTotal;
    }

    public List<BillTypeAtomic> getBillTypesForNetCreditTotal() {
        if (billTypesForNetCreditTotal == null) {
            billTypesForNetCreditTotal = new ArrayList<>();
            billTypesForNetCreditTotal.addAll(getBillTypesForCollectedCredit());
            billTypesForNetCreditTotal.addAll(getBillTypesForRefundedCredit());
            // Consider if any filtering is needed to prevent double-counting in net calculations
        }
        return billTypesForNetCreditTotal;
    }

    public List<PaymentMethod> getPaymentMethodsForCollectedCreditCard() {
        if (paymentMethodsForCollectedCreditCard == null) {
            paymentMethodsForCollectedCreditCard = new ArrayList<>();
            paymentMethodsForCollectedCreditCard.add(PaymentMethod.Card); // Assuming 'Card' represents credit card payments
            // Add any other payment methods if different types of cards need to be accounted for
        }
        return paymentMethodsForCollectedCreditCard;
    }

    public List<BillTypeAtomic> getBillTypesForCollectedCreditCard() {
        if (billTypesForCollectedCreditCard == null) {
            billTypesForCollectedCreditCard = new ArrayList<>();
            // Add BillTypeAtomic entries that represent credit card collections
            // Assuming CHANNEL_BOOKING_WITH_PAYMENT and OPD_BILL_WITH_PAYMENT are collected via credit card
            billTypesForCollectedCreditCard.add(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
            billTypesForCollectedCreditCard.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            billTypesForCollectedCreditCard.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
            // Add other BillTypeAtomic entries if they are collected via credit card
        }
        return billTypesForCollectedCreditCard;
    }

    public List<PaymentMethod> getPaymentMethodsForRefundedCreditCard() {
        if (paymentMethodsForRefundedCreditCard == null) {
            paymentMethodsForRefundedCreditCard = new ArrayList<>();
            paymentMethodsForRefundedCreditCard.add(PaymentMethod.Card); // Assuming 'Card' covers all types of card payments including refunds
            // Add any other payment methods if different types of card refunds need to be accounted for
        }
        return paymentMethodsForRefundedCreditCard;
    }

    public List<BillTypeAtomic> getBillTypesForRefundedCreditCard() {
        if (billTypesForRefundedCreditCard == null) {
            billTypesForRefundedCreditCard = new ArrayList<>();
            // Add BillTypeAtomic entries that represent credit card refunds
            // Assuming CHANNEL_REFUND is refunded via credit card 
            billTypesForRefundedCreditCard.addAll(BillTypeAtomic.findByCategory(BillCategory.REFUND));
            billTypesForRefundedCreditCard.addAll(BillTypeAtomic.findByCategory(BillCategory.CANCELLATION));
            // Add other BillTypeAtomic entries if they are refunded via credit card
        }
        return billTypesForRefundedCreditCard;
    }

    public List<PaymentMethod> getPaymentMethodsForNetCreditCardTotal() {
        if (paymentMethodsForNetCreditCardTotal == null) {
            paymentMethodsForNetCreditCardTotal = new ArrayList<>();
            paymentMethodsForNetCreditCardTotal.addAll(getPaymentMethodsForCollectedCreditCard());
            paymentMethodsForNetCreditCardTotal.addAll(getPaymentMethodsForRefundedCreditCard());
            // Since it's a net total, consider removing duplicates if necessary
        }
        return paymentMethodsForNetCreditCardTotal;
    }

    public List<BillTypeAtomic> getBillTypesForNetCreditCardTotal() {
        if (billTypesForNetCreditCardTotal == null) {
            billTypesForNetCreditCardTotal = new ArrayList<>();
            billTypesForNetCreditCardTotal.addAll(getBillTypesForCollectedCreditCard());
            billTypesForNetCreditCardTotal.addAll(getBillTypesForRefundedCreditCard());
            // Consider if any filtering is needed to prevent double-counting in net calculations
        }
        return billTypesForNetCreditCardTotal;
    }

    public List<PaymentMethod> getPaymentMethodsForCollectedDebitCard() {
        if (paymentMethodsForCollectedDebitCard == null) {
            paymentMethodsForCollectedDebitCard = new ArrayList<>();
            paymentMethodsForCollectedDebitCard.add(PaymentMethod.Credit); // Assuming 'Card' also covers debit card collections
            // If your application differentiates between credit and debit cards, you may need a specific entry for debit cards
        }
        return paymentMethodsForCollectedDebitCard;
    }

    public List<BillTypeAtomic> getBillTypesForCollectedDebitCard() {
        if (billTypesForCollectedDebitCard == null) {
            billTypesForCollectedDebitCard = new ArrayList<>();
            // Add BillTypeAtomic entries that represent debit card collections
            // Assuming specific services like CHANNEL_BOOKING_WITH_PAYMENT are paid for with debit cards
            //billTypesForCollectedDebitCard.add(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
            // Add other BillTypeAtomic entries if they are typically paid with a debit card
        }
        return billTypesForCollectedDebitCard;
    }

    public List<PaymentMethod> getPaymentMethodsForRefundedDebitCard() {
        if (paymentMethodsForRefundedDebitCard == null) {
            paymentMethodsForRefundedDebitCard = new ArrayList<>();
            paymentMethodsForRefundedDebitCard.add(PaymentMethod.Card); // Assuming 'Card' also covers debit card refunds
            // Again, if your application distinguishes between credit and debit cards for refunds, adjust accordingly
        }
        return paymentMethodsForRefundedDebitCard;
    }

    public List<BillTypeAtomic> getBillTypesForRefundedDebitCard() {
        if (billTypesForRefundedDebitCard == null) {
            billTypesForRefundedDebitCard = new ArrayList<>();
            // Add BillTypeAtomic entries related to debit card refunds
            // Assuming CHANNEL_REFUND might be refunded to a debit card
            billTypesForRefundedDebitCard.add(BillTypeAtomic.CHANNEL_REFUND);
            // Include any other applicable BillTypeAtomic entries for debit card refunds
        }
        return billTypesForRefundedDebitCard;
    }

    public List<PaymentMethod> getPaymentMethodsForNetDebitCardTotal() {
        if (paymentMethodsForNetDebitCardTotal == null) {
            paymentMethodsForNetDebitCardTotal = new ArrayList<>();
            paymentMethodsForNetDebitCardTotal.addAll(getPaymentMethodsForCollectedDebitCard());
            paymentMethodsForNetDebitCardTotal.addAll(getPaymentMethodsForRefundedDebitCard());
            // Consider removing duplicates if necessary, as this is for net calculation
        }
        return paymentMethodsForNetDebitCardTotal;
    }

    public List<BillTypeAtomic> getBillTypesForNetDebitCardTotal() {
        if (billTypesForNetDebitCardTotal == null) {
            billTypesForNetDebitCardTotal = new ArrayList<>();
            billTypesForNetDebitCardTotal.addAll(getBillTypesForCollectedDebitCard());
            billTypesForNetDebitCardTotal.addAll(getBillTypesForRefundedDebitCard());
            // Consider whether any filtering is needed to avoid double-counting
        }
        return billTypesForNetDebitCardTotal;
    }

    public List<PaymentMethod> getPaymentMethodsForCollectedVoucher() {
        if (paymentMethodsForCollectedVoucher == null) {
            paymentMethodsForCollectedVoucher = new ArrayList<>();
            paymentMethodsForCollectedVoucher.add(PaymentMethod.Voucher); // Assuming 'Voucher' covers all voucher collections
            // Add any other payment methods if different types of vouchers need to be accounted for
        }
        return paymentMethodsForCollectedVoucher;
    }

    public List<BillTypeAtomic> getBillTypesForCollectedVoucher() {
        if (billTypesForCollectedVoucher == null) {
            billTypesForCollectedVoucher = new ArrayList<>();
            // Add BillTypeAtomic entries that represent voucher collections
            // Assuming specific services are payable with vouchers, add them here
            // Example: 
            // billTypesForCollectedVoucher.add(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT);
            // Adjust according to your specific use case
        }
        return billTypesForCollectedVoucher;
    }

    public List<PaymentMethod> getPaymentMethodsForRefundedVoucher() {
        if (paymentMethodsForRefundedVoucher == null) {
            paymentMethodsForRefundedVoucher = new ArrayList<>();
            paymentMethodsForRefundedVoucher.add(PaymentMethod.Voucher); // Assuming 'Voucher' also covers refunds
            // Add any other relevant payment methods if your application has more specific types of voucher refunds
        }
        return paymentMethodsForRefundedVoucher;
    }

    public List<BillTypeAtomic> getBillTypesForRefundedVoucher() {
        if (billTypesForRefundedVoucher == null) {
            billTypesForRefundedVoucher = new ArrayList<>();
            // Add BillTypeAtomic entries related to voucher refunds
            // Example:
            // billTypesForRefundedVoucher.add(BillTypeAtomic.CHANNEL_REFUND);
            // Adjust based on your specific use case and which transactions involve voucher refunds
        }
        return billTypesForRefundedVoucher;
    }

    public List<PaymentMethod> getPaymentMethodsForNetVoucherTotal() {
        if (paymentMethodsForNetVoucherTotal == null) {
            paymentMethodsForNetVoucherTotal = new ArrayList<>();
            paymentMethodsForNetVoucherTotal.addAll(getPaymentMethodsForCollectedVoucher());
            paymentMethodsForNetVoucherTotal.addAll(getPaymentMethodsForRefundedVoucher());
            // Since it's a net calculation, consider removing duplicates if necessary
        }
        return paymentMethodsForNetVoucherTotal;
    }

    public List<BillTypeAtomic> getBillTypesForNetVoucherTotal() {
        if (billTypesForNetVoucherTotal == null) {
            billTypesForNetVoucherTotal = new ArrayList<>();
            billTypesForNetVoucherTotal.addAll(getBillTypesForCollectedVoucher());
            billTypesForNetVoucherTotal.addAll(getBillTypesForRefundedVoucher());
            // Consider whether any filtering is needed to avoid double-counting in net calculations
        }
        return billTypesForNetVoucherTotal;
    }

    public List<PaymentMethod> getPaymentMethodsForFloatCollected() {
        if (paymentMethodsForFloatCollected == null) {
            paymentMethodsForFloatCollected = new ArrayList<>();
            // Float collection might not be tied to a specific payment method, but for completeness:
            paymentMethodsForFloatCollected.add(PaymentMethod.Cash);
            // Add any other relevant methods if applicable
        }
        return paymentMethodsForFloatCollected;
    }

    public List<BillTypeAtomic> getBillTypesForFloatCollected() {
        if (billTypesForFloatCollected == null) {
            billTypesForFloatCollected = new ArrayList<>();
            billTypesForFloatCollected.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_INCREASE));
        }
        return billTypesForFloatCollected;
    }

    public List<PaymentMethod> getPaymentMethodsForFloatHandover() {
        if (paymentMethodsForFloatHandover == null) {
            paymentMethodsForFloatHandover = new ArrayList<>();

        }
        return paymentMethodsForFloatHandover;
    }

    public List<BillTypeAtomic> getBillTypesForFloatHandover() {
        if (billTypesForFloatHandover == null) {
            billTypesForFloatHandover = new ArrayList<>();
            billTypesForFloatHandover.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_DECREASE));
        }
        return billTypesForFloatHandover;
    }

    public List<PaymentMethod> getPaymentMethodsForFloatMySafe() {
        if (paymentMethodsForFloatMySafe == null) {
            paymentMethodsForFloatMySafe = new ArrayList<>();
            paymentMethodsForFloatMySafe.addAll(PaymentMethod.getActivePaymentMethods()); // Presuming cash management is part of 'My Safe' operations
        }
        return paymentMethodsForFloatMySafe;
    }

    public List<BillTypeAtomic> getBillTypesForFloatMySafe() {
        if (billTypesForFloatMySafe == null) {
            billTypesForFloatMySafe = new ArrayList<>();
            billTypesForFloatMySafe.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_STARTING_BALANCE));
        }
        return billTypesForFloatMySafe;
    }

    public List<PaymentMethod> getPaymentMethodsForShiftEndHandovers() {
        if (paymentMethodsForShiftEndHandovers == null) {
            paymentMethodsForShiftEndHandovers = new ArrayList<>();
            // Collection handover might primarily involve cash, but other methods can be included as needed
            paymentMethodsForShiftEndHandovers.add(PaymentMethod.Cash);
            // Add any other relevant methods if applicable
        }
        return paymentMethodsForShiftEndHandovers;
    }

    public List<BillTypeAtomic> getBillTypesOfShiftEndHandovers() {
        if (billTypesOfShiftEndHandovers == null) {
            billTypesOfShiftEndHandovers = new ArrayList<>();
            billTypesOfShiftEndHandovers.add(BillTypeAtomic.FUND_SHIFT_END_BILL);
        }
        return billTypesOfShiftEndHandovers;
    }

    public List<PaymentMethod> getPaymentMethodsForCashCollectedTransferIn() {
        if (paymentMethodsForCashCollectedTransferIn == null) {
            paymentMethodsForCashCollectedTransferIn = new ArrayList<>();
            // Cash collected through transfers might still be categorized under Cash or a specific transfer method
            paymentMethodsForCashCollectedTransferIn.add(PaymentMethod.Cash);
            // Include specific transfer methods if applicable
        }
        return paymentMethodsForCashCollectedTransferIn;
    }

    public List<BillTypeAtomic> getBillTypesForCashCollectedTransferIn() {
        if (billTypesForCashCollectedTransferIn == null) {
            billTypesForCashCollectedTransferIn = new ArrayList<>();
            // Assuming specific transactions associated with cash collected through transfers
            billTypesForCashCollectedTransferIn.add(BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL);
        }
        return billTypesForCashCollectedTransferIn;
    }

    public List<PaymentMethod> getPaymentMethodsForBankWithdrawals() {
        if (paymentMethodsForBankWithdrawals == null) {
            paymentMethodsForBankWithdrawals = new ArrayList<>();
            // Bank withdrawals might not be directly linked to a payment method as they are a form of fund sourcing
            paymentMethodsForBankWithdrawals.add(PaymentMethod.Cash); // Assuming withdrawals are primarily in cash
            // This list might be unnecessary depending on your system's categorization of withdrawals as a 'payment method'
        }
        return paymentMethodsForBankWithdrawals;
    }

    public List<BillTypeAtomic> getBillTypesForBankWithdrawals() {
        if (billTypesForBankWithdrawals == null) {
            billTypesForBankWithdrawals = new ArrayList<>();
            billTypesForBankWithdrawals.add(BillTypeAtomic.FUND_WITHDRAWAL_BILL);
        }
        return billTypesForBankWithdrawals;
    }

    public List<PaymentMethod> getPaymentMethodsForReceivedFromCollectionCenter() {
        if (paymentMethodsForReceivedFromCollectionCenter == null) {
            paymentMethodsForReceivedFromCollectionCenter = new ArrayList<>();
            // Assuming funds received from collection centers might primarily involve cash or checks
            paymentMethodsForReceivedFromCollectionCenter.add(PaymentMethod.Cash);
            paymentMethodsForReceivedFromCollectionCenter.add(PaymentMethod.Cheque);
            // Include other methods as applicable
        }
        return paymentMethodsForReceivedFromCollectionCenter;
    }

    public List<BillTypeAtomic> getBillTypesForReceivedFromCollectionCenter() {
        if (billTypesForReceivedFromCollectionCenter == null) {
            billTypesForReceivedFromCollectionCenter = new ArrayList<>();
            // Transactions related to receiving funds from collection centers
            // Example: billTypesForReceivedFromCollectionCenter.add(BillTypeAtomic.CC_BILL);
            // Adjust to match the specific transactions in your application
        }
        return billTypesForReceivedFromCollectionCenter;
    }

    public List<PaymentMethod> getPaymentMethodsForTotalShiftCashIn() {
        if (paymentMethodsForTotalShiftCashIn == null) {
            paymentMethodsForTotalShiftCashIn = new ArrayList<>();
            // This may aggregate all methods contributing to the shift's cash intake
            paymentMethodsForTotalShiftCashIn.add(PaymentMethod.Cash);
            paymentMethodsForTotalShiftCashIn.add(PaymentMethod.Cheque);
            paymentMethodsForTotalShiftCashIn.add(PaymentMethod.Card);
            // And so on for all relevant payment methods
        }
        return paymentMethodsForTotalShiftCashIn;
    }

    public List<BillTypeAtomic> getBillTypesForTotalShiftCashIn() {
        if (billTypesForTotalShiftCashIn == null) {
            billTypesForTotalShiftCashIn = new ArrayList<>();
            // May include a wide range of transactions contributing to the shift's cash intake
            // Example: billTypesForTotalShiftCashIn.addAll(getBillTypesForReceivedFromCollectionCenter());
            // Further adjust and populate as needed
        }
        return billTypesForTotalShiftCashIn;
    }

    public List<PaymentMethod> getPaymentMethodsForDrPayments() {
        if (paymentMethodsForDrPayments == null) {
            paymentMethodsForDrPayments = new ArrayList<>();
            // Assuming doctor payments might primarily involve transfers or checks
            paymentMethodsForDrPayments.add(PaymentMethod.Cheque);
            paymentMethodsForDrPayments.add(PaymentMethod.OnlineSettlement);
            // Include other methods as applicable
        }
        return paymentMethodsForDrPayments;
    }

    public List<BillTypeAtomic> getBillTypesForDrPayments() {
        if (billTypesForDrPayments == null) {
            billTypesForDrPayments = new ArrayList<>();
            // Specific transactions related to payments made to doctors
            billTypesForDrPayments.add(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES);
            // Include other BillTypeAtomics as relevant to your doctor payment processing
        }
        return billTypesForDrPayments;
    }

    public List<PaymentMethod> getPaymentMethodsForOtherPayments() {
        if (paymentMethodsForOtherPayments == null) {
            paymentMethodsForOtherPayments = new ArrayList<>();
            // "Other Payments" can encompass a variety of methods depending on your operational definitions
            paymentMethodsForOtherPayments.add(PaymentMethod.Cash);
            paymentMethodsForOtherPayments.add(PaymentMethod.Cheque);
            // Add more as applicable to your system's practices
        }
        return paymentMethodsForOtherPayments;
    }

    public List<BillTypeAtomic> getBillTypesForOtherPayments() {
        if (billTypesForOtherPayments == null) {
            billTypesForOtherPayments = new ArrayList<>();
            // This would include any BillTypeAtomic not covered by more specific categories
            // Example:
            // billTypesForOtherPayments.add(BillTypeAtomic.PHARMACY_ADJUSTMENT);
            // Adjust according to your system's categorization of "Other Payments"
        }
        return billTypesForOtherPayments;
    }

    public List<PaymentMethod> getPaymentMethodsForCashGivenOutTransferOut() {
        if (paymentMethodsForCashGivenOutTransferOut == null) {
            paymentMethodsForCashGivenOutTransferOut = new ArrayList<>();
            // Cash given out for transfers might typically involve cash, but could include other methods
            paymentMethodsForCashGivenOutTransferOut.add(PaymentMethod.Cash);
            // Include any other relevant methods
        }
        return paymentMethodsForCashGivenOutTransferOut;
    }

    public List<BillTypeAtomic> getBillTypesForCashGivenOutTransferOut() {
        if (billTypesForCashGivenOutTransferOut == null) {
            billTypesForCashGivenOutTransferOut = new ArrayList<>();
            // Include specific transactions related to cash given out for transfers
            billTypesForCashGivenOutTransferOut.add(BillTypeAtomic.FUND_TRANSFER_BILL);
            // Adjust as needed for your operations
        }
        return billTypesForCashGivenOutTransferOut;
    }

    public List<PaymentMethod> getPaymentMethodsForBankDeposits() {
        if (paymentMethodsForBankDeposits == null) {
            paymentMethodsForBankDeposits = new ArrayList<>();
            // Bank deposits may primarily involve direct bank transactions
            paymentMethodsForBankDeposits.add(PaymentMethod.OnlineSettlement);
            // Consider including other methods if your system accounts for bank deposits differently
        }
        return paymentMethodsForBankDeposits;
    }

    public List<BillTypeAtomic> getBillTypesForBankDeposits() {
        if (billTypesForBankDeposits == null) {
            billTypesForBankDeposits = new ArrayList<>();
            // Transactions specifically related to bank deposits
            billTypesForBankDeposits.add(BillTypeAtomic.FUND_DEPOSIT_BILL);
            // Adjust to include all relevant BillTypeAtomic entries for your bank deposit processes
        }
        return billTypesForBankDeposits;
    }

    public List<PaymentMethod> getPaymentMethodsForShortExcess() {
        if (paymentMethodsForShortExcess == null) {
            paymentMethodsForShortExcess = new ArrayList<>();
            // Short/excess might not directly relate to a specific payment method but for completeness:
            paymentMethodsForShortExcess.add(PaymentMethod.Cash); // Typically related to cash handling
            // Consider other methods as needed, though this category may be more about adjustment entries
        }
        return paymentMethodsForShortExcess;
    }

    public List<BillTypeAtomic> getBillTypesForShortExcess() {
        if (billTypesForShortExcess == null) {
            billTypesForShortExcess = new ArrayList<>();
            // Include specific transactions related to short/excess adjustments
            billTypesForShortExcess.add(BillTypeAtomic.PHARMACY_ADJUSTMENT);
            // Adjust according to the transactions your system uses for handling shorts/excesses
        }
        return billTypesForShortExcess;
    }

}
