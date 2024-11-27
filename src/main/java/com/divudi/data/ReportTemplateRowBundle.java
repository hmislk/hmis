package com.divudi.data;

import com.divudi.bean.common.SessionController;

import static com.divudi.data.PaymentMethod.Agent;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.IOU;
import static com.divudi.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import static com.divudi.data.PaymentMethod.PatientDeposit;
import static com.divudi.data.PaymentMethod.PatientPoints;
import static com.divudi.data.PaymentMethod.Slip;
import static com.divudi.data.PaymentMethod.Staff;
import static com.divudi.data.PaymentMethod.Staff_Welfare;
import static com.divudi.data.PaymentMethod.Voucher;
import static com.divudi.data.PaymentMethod.YouOweMe;
import static com.divudi.data.PaymentMethod.ewallet;

import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.ReportTemplate;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.DenominationTransaction;
import com.divudi.entity.channel.SessionInstance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * @author buddhika
 */
public class ReportTemplateRowBundle implements Serializable {

    private static final long serialVersionUID = 1L;

    // UUID field to uniquely identify each object
    private UUID id;

    //    private SessionController sessionController;
    private List<com.divudi.entity.cashTransaction.Denomination> denominations;

    private List<ReportTemplateRowBundle> bundles;
    List<DenominationTransaction> denominationTransactions;
    private ReportTemplate reportTemplate;
    private List<ReportTemplateRow> reportTemplateRows;

    private Double grossTotal;
    private Double discount;
    private Double total;
    private Double tax;

    private Double hospitalTotal;
    private Double staffTotal;
    private Double ccTotal;

    private Double totalIn;
    private Double totalOut;
    private Long countIn;
    private Long countOut;
    private Long count;
    private String name;
    private String bundleType;
    private String description;
    private SessionInstance sessionInstance;
    private Long long1;
    private Long long2;
    private Long long3;
    private Long long4;
    private Long long5;
    private Long long6;
    private Long long7;
    private Long long8;
    private Long long9;
    private Long long10;

    private PaymentMethod paymentMethod;

    private double onCallValue;
    private double cashValue;
    private double cardValue;
    private double multiplePaymentMethodsValue;
    private double staffValue;
    private double creditValue;
    private double staffWelfareValue;
    private double voucherValue;
    private double iouValue;
    private double agentValue;
    private double chequeValue;
    private double slipValue;
    private double eWalletValue;
    private double patientDepositValue;
    private double patientPointsValue;
    private double onlineSettlementValue;

    private Boolean selectAllCashToHandover;

    private double onCallHandoverValue;
    private double cashHandoverValue;
    private double denominatorValue;
    private double cardHandoverValue;
    private double multiplePaymentMethodsHandoverValue;
    private double staffHandoverValue;
    private double creditHandoverValue;
    private double staffWelfareHandoverValue;
    private double voucherHandoverValue;
    private double iouHandoverValue;
    private double agentHandoverValue;
    private double chequeHandoverValue;
    private double slipHandoverValue;
    private double eWalletHandoverValue;
    private double patientDepositHandoverValue;
    private double patientPointsHandoverValue;
    private double onlineSettlementHandoverValue;

    // Booleans to track transactions
    private boolean hasOnCallTransaction;
    private boolean hasCashTransaction;
    private boolean hasCardTransaction;
    private boolean hasMultiplePaymentMethodsTransaction;
    private boolean hasStaffTransaction;
    private boolean hasCreditTransaction;
    private boolean hasStaffWelfareTransaction;
    private boolean hasVoucherTransaction;
    private boolean hasIouTransaction;
    private boolean hasAgentTransaction;
    private boolean hasChequeTransaction;
    private boolean hasSlipTransaction;
    private boolean hasEWalletTransaction;
    private boolean hasPatientDepositTransaction;
    private boolean hasPatientPointsTransaction;
    private boolean hasOnlineSettlementTransaction;

    private WebUser user;
    private WebUser fromUser;
    private WebUser toUser;
    private Date date;
    private Department department;
    private List<Department> departments;
    private Bill startBill;
    private Bill endBill;

    private PaymentHandover paymentHandover;

    private boolean selected;

    public ReportTemplateRowBundle() {
        this.id = UUID.randomUUID();
    }

    //    public ReportTemplateRowBundle(SessionController sessionController) {
//        this();
//        this.sessionController = sessionController;
//    }
    private double nullSafeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    public void selectAllChildBundles() {
        for (ReportTemplateRowBundle b : getBundles()) {
            b.setSelected(true);
        }
    }

    private void resetTotals() {
        grossTotal = 0.0;
        discount = 0.0;
        total = 0.0;
        hospitalTotal = 0.0;
        staffTotal = 0.0;
        ccTotal = 0.0;
        totalIn = 0.0;
        totalOut = 0.0;
        countIn = 0L;
        countOut = 0L;
        count = 0L;

        onCallValue = 0.0;
        cashValue = 0.0;
        cardValue = 0.0;
        multiplePaymentMethodsValue = 0.0;
        staffValue = 0.0;
        creditValue = 0.0;
        staffWelfareValue = 0.0;
        voucherValue = 0.0;
        iouValue = 0.0;
        agentValue = 0.0;
        chequeValue = 0.0;
        slipValue = 0.0;
        eWalletValue = 0.0;
        patientDepositValue = 0.0;
        patientPointsValue = 0.0;
        onlineSettlementValue = 0.0;

        onCallHandoverValue = 0.0;
        cashHandoverValue = 0.0;
        denominatorValue = 0.0;
        cardHandoverValue = 0.0;
        multiplePaymentMethodsHandoverValue = 0.0;
        staffHandoverValue = 0.0;
        creditHandoverValue = 0.0;
        staffWelfareHandoverValue = 0.0;
        voucherHandoverValue = 0.0;
        iouHandoverValue = 0.0;
        agentHandoverValue = 0.0;
        chequeHandoverValue = 0.0;
        slipHandoverValue = 0.0;
        eWalletHandoverValue = 0.0;
        patientDepositHandoverValue = 0.0;
        patientPointsHandoverValue = 0.0;
        onlineSettlementHandoverValue = 0.0;

        hasOnCallTransaction = false;
        hasCashTransaction = false;
        hasCardTransaction = false;
        hasMultiplePaymentMethodsTransaction = false;
        hasStaffTransaction = false;
        hasCreditTransaction = false;
        hasStaffWelfareTransaction = false;
        hasVoucherTransaction = false;
        hasIouTransaction = false;
        hasAgentTransaction = false;
        hasChequeTransaction = false;
        hasSlipTransaction = false;
        hasEWalletTransaction = false;
        hasPatientDepositTransaction = false;
        hasPatientPointsTransaction = false;
        hasOnlineSettlementTransaction = false;
    }

    public void collectDepartments() {
        Set<Department> uniqueDepartments = new HashSet<>();
        if (bundles != null) {
            for (ReportTemplateRowBundle b : bundles) {
                if (b.getDepartment() != null) {
                    uniqueDepartments.add(b.getDepartment());
                }
            }
        }
        departments = new ArrayList<>(uniqueDepartments);
    }

    public void aggregateTotalsFromAllChildBundles() {
        resetTotals(); // Resets all totals before computation

        if (bundles != null) {
            for (ReportTemplateRowBundle childBundle : bundles) {
                grossTotal += nullSafeDouble(childBundle.grossTotal);
                discount += nullSafeDouble(childBundle.discount);
                total += nullSafeDouble(childBundle.total);
                hospitalTotal += nullSafeDouble(childBundle.hospitalTotal);
                staffTotal += nullSafeDouble(childBundle.staffTotal);
                ccTotal += nullSafeDouble(childBundle.ccTotal);
                totalIn += nullSafeDouble(childBundle.totalIn);
                totalOut += nullSafeDouble(childBundle.totalOut);

                // Increment counts
                countIn += childBundle.countIn != null ? childBundle.countIn : 0;
                countOut += childBundle.countOut != null ? childBundle.countOut : 0;
                count += childBundle.count != null ? childBundle.count : 0;

                // Payment values
                onCallValue += childBundle.onCallValue;
                cashValue += childBundle.cashValue;
                cardValue += childBundle.cardValue;
                multiplePaymentMethodsValue += childBundle.multiplePaymentMethodsValue;
                staffValue += childBundle.staffValue;
                creditValue += childBundle.creditValue;
                staffWelfareValue += childBundle.staffWelfareValue;
                voucherValue += childBundle.voucherValue;
                iouValue += childBundle.iouValue;
                agentValue += childBundle.agentValue;
                chequeValue += childBundle.chequeValue;
                slipValue += childBundle.slipValue;
                eWalletValue += childBundle.eWalletValue;
                patientDepositValue += childBundle.patientDepositValue;
                patientPointsValue += childBundle.patientPointsValue;
                onlineSettlementValue += childBundle.onlineSettlementValue;

                onCallHandoverValue += childBundle.onCallHandoverValue;
                cashHandoverValue += childBundle.cashHandoverValue;
                cardHandoverValue += childBundle.cardHandoverValue;
                multiplePaymentMethodsHandoverValue += childBundle.multiplePaymentMethodsHandoverValue;
                staffHandoverValue += childBundle.staffHandoverValue;
                creditHandoverValue += childBundle.creditHandoverValue;
                staffWelfareHandoverValue += childBundle.staffWelfareHandoverValue;
                voucherHandoverValue += childBundle.voucherHandoverValue;
                iouHandoverValue += childBundle.iouHandoverValue;
                agentHandoverValue += childBundle.agentHandoverValue;
                chequeHandoverValue += childBundle.chequeHandoverValue;
                slipHandoverValue += childBundle.slipHandoverValue;
                eWalletHandoverValue += childBundle.eWalletHandoverValue;
                patientDepositHandoverValue += childBundle.patientDepositHandoverValue;
                patientPointsHandoverValue += childBundle.patientPointsHandoverValue;
                onlineSettlementHandoverValue += childBundle.onlineSettlementHandoverValue;

                // Aggregate flags
                hasOnCallTransaction |= childBundle.hasOnCallTransaction;
                hasCashTransaction |= childBundle.hasCashTransaction;
                hasCardTransaction |= childBundle.hasCardTransaction;
                hasMultiplePaymentMethodsTransaction |= childBundle.hasMultiplePaymentMethodsTransaction;
                hasStaffTransaction |= childBundle.hasStaffTransaction;
                hasCreditTransaction |= childBundle.hasCreditTransaction;
                hasStaffWelfareTransaction |= childBundle.hasStaffWelfareTransaction;
                hasVoucherTransaction |= childBundle.hasVoucherTransaction;
                hasIouTransaction |= childBundle.hasIouTransaction;
                hasAgentTransaction |= childBundle.hasAgentTransaction;
                hasChequeTransaction |= childBundle.hasChequeTransaction;
                hasSlipTransaction |= childBundle.hasSlipTransaction;
                hasEWalletTransaction |= childBundle.hasEWalletTransaction;
                hasPatientDepositTransaction |= childBundle.hasPatientDepositTransaction;
                hasPatientPointsTransaction |= childBundle.hasPatientPointsTransaction;
                hasOnlineSettlementTransaction |= childBundle.hasOnlineSettlementTransaction;
            }
        }
    }

    public ReportTemplateRowBundle createBundleByAggregatingMonthlyTotalsFromBills() {
        ReportTemplateRowBundle newlyCreatedBundle = new ReportTemplateRowBundle();
        Map<String, ReportTemplateRow> monthlyTotalsMap = new HashMap<>();

        for (ReportTemplateRow row : this.getReportTemplateRows()) {
            if (row.getBill() == null) {
                continue;
            }

            // Extract date and financial data from the bill
            Date date = row.getBill().getCreatedAt();
            Double grossTotal = row.getBill().getTotal();
            Double tax = row.getBill().getTax();
            Double netTotal = row.getBill().getNetTotal();

            // Convert Date to LocalDate to extract month and year
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int month = localDate.getMonthValue();
            int year = localDate.getYear();

            // Create a unique key for each month-year combination
            String key = year + "-" + month;

            // Retrieve or create the ReportTemplateRow for the current month
            ReportTemplateRow monthRow = monthlyTotalsMap.get(key);
            if (monthRow == null) {
                monthRow = new ReportTemplateRow();
                // Set the date to the first day of the month
                LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
                Date firstDayDate = Date.from(firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());
                monthRow.setDate(firstDayDate);
                monthRow.setTotal(0.0);
                monthRow.setTax(0.0);
                monthRow.setGrossTotal(0.0);
                monthlyTotalsMap.put(key, monthRow);
            }

            // Aggregate the totals
            monthRow.setTotal(monthRow.getTotal() + netTotal);
            monthRow.setTax(monthRow.getTax() + tax);
            monthRow.setGrossTotal(monthRow.getGrossTotal() + grossTotal);
        }

        // Collect the aggregated monthly totals into a list
        List<ReportTemplateRow> monthlyTotalRows = new ArrayList<>(monthlyTotalsMap.values());
        monthlyTotalRows.sort(Comparator.comparing(ReportTemplateRow::getDate));
        newlyCreatedBundle.setReportTemplateRows(monthlyTotalRows);
        return newlyCreatedBundle;
    }

    public ReportTemplateRowBundle createBundleByAggregatingConsultantTotalsFromBills() {
        ReportTemplateRowBundle newlyCreatedBundle = new ReportTemplateRowBundle();
        Map<Long, ReportTemplateRow> staffTotalsMap = new HashMap<>();

        for (ReportTemplateRow row : this.getReportTemplateRows()) {
            if (row.getBill() == null) {
                continue;
            }

            // Extract financial data from the bill
            Double grossTotal = row.getBill().getTotal();
            Double tax = row.getBill().getTax();
            Double netTotal = row.getBill().getNetTotal();
            Staff staff = row.getBill().getStaff();

            if (staff == null) {
                continue; // Skip if no staff assigned
            }

            Long staffId = staff.getId();

            // Retrieve or create the ReportTemplateRow for the current staff
            ReportTemplateRow staffRow = staffTotalsMap.get(staffId);
            if (staffRow == null) {
                staffRow = new ReportTemplateRow();
                staffRow.setStaff(staff);
                staffRow.setTotal(0.0);
                staffRow.setTax(0.0);
                staffRow.setGrossTotal(0.0);
                staffTotalsMap.put(staffId, staffRow);
            }

            // Aggregate the totals
            staffRow.setTotal(staffRow.getTotal() + netTotal);
            staffRow.setTax(staffRow.getTax() + tax);
            staffRow.setGrossTotal(staffRow.getGrossTotal() + grossTotal);
        }

        // Collect the aggregated staff totals into a list
        List<ReportTemplateRow> staffTotalRows = new ArrayList<>(staffTotalsMap.values());
        // Sort the list by staff name
        Collections.sort(staffTotalRows, new Comparator<ReportTemplateRow>() {
            public int compare(ReportTemplateRow r1, ReportTemplateRow r2) {
                return r1.getStaff().getPerson().getName().compareTo(r2.getStaff().getPerson().getName());
            }
        });
        newlyCreatedBundle.setReportTemplateRows(staffTotalRows);
        return newlyCreatedBundle;
    }

    public void aggregateTotalsFromSelectedChildBundles() {
        resetTotals(); // Resets all totals before computation

        if (bundles != null) {
            for (ReportTemplateRowBundle childBundle : bundles) {
                if (childBundle.isSelected()) {
                    grossTotal += nullSafeDouble(childBundle.grossTotal);
                    discount += nullSafeDouble(childBundle.discount);
                    total += nullSafeDouble(childBundle.total);
                    hospitalTotal += nullSafeDouble(childBundle.hospitalTotal);
                    staffTotal += nullSafeDouble(childBundle.staffTotal);
                    ccTotal += nullSafeDouble(childBundle.ccTotal);
                    totalIn += nullSafeDouble(childBundle.totalIn);
                    totalOut += nullSafeDouble(childBundle.totalOut);

                    // Increment counts
                    countIn += childBundle.countIn != null ? childBundle.countIn : 0;
                    countOut += childBundle.countOut != null ? childBundle.countOut : 0;
                    count += childBundle.count != null ? childBundle.count : 0;

                    // Payment values
                    onCallValue += childBundle.onCallValue;
                    cashValue += childBundle.cashValue;
                    cardValue += childBundle.cardValue;
                    multiplePaymentMethodsValue += childBundle.multiplePaymentMethodsValue;
                    staffValue += childBundle.staffValue;
                    creditValue += childBundle.creditValue;
                    staffWelfareValue += childBundle.staffWelfareValue;
                    voucherValue += childBundle.voucherValue;
                    iouValue += childBundle.iouValue;
                    agentValue += childBundle.agentValue;
                    chequeValue += childBundle.chequeValue;
                    slipValue += childBundle.slipValue;
                    eWalletValue += childBundle.eWalletValue;
                    patientDepositValue += childBundle.patientDepositValue;
                    patientPointsValue += childBundle.patientPointsValue;
                    onlineSettlementValue += childBundle.onlineSettlementValue;

                    // Handover values
                    onCallHandoverValue += childBundle.onCallHandoverValue;
                    cashHandoverValue += childBundle.cashHandoverValue;
                    cardHandoverValue += childBundle.cardHandoverValue;
                    multiplePaymentMethodsHandoverValue += childBundle.multiplePaymentMethodsHandoverValue;
                    staffHandoverValue += childBundle.staffHandoverValue;
                    creditHandoverValue += childBundle.creditHandoverValue;
                    staffWelfareHandoverValue += childBundle.staffWelfareHandoverValue;
                    voucherHandoverValue += childBundle.voucherHandoverValue;
                    iouHandoverValue += childBundle.iouHandoverValue;
                    agentHandoverValue += childBundle.agentHandoverValue;
                    chequeHandoverValue += childBundle.chequeHandoverValue;
                    slipHandoverValue += childBundle.slipHandoverValue;
                    eWalletHandoverValue += childBundle.eWalletHandoverValue;
                    patientDepositHandoverValue += childBundle.patientDepositHandoverValue;
                    patientPointsHandoverValue += childBundle.patientPointsHandoverValue;
                    onlineSettlementHandoverValue += childBundle.onlineSettlementHandoverValue;
                }

                // Aggregate flags
                hasOnCallTransaction |= childBundle.hasOnCallTransaction;
                hasCashTransaction |= childBundle.hasCashTransaction;
                hasCardTransaction |= childBundle.hasCardTransaction;
                hasMultiplePaymentMethodsTransaction |= childBundle.hasMultiplePaymentMethodsTransaction;
                hasStaffTransaction |= childBundle.hasStaffTransaction;
                hasCreditTransaction |= childBundle.hasCreditTransaction;
                hasStaffWelfareTransaction |= childBundle.hasStaffWelfareTransaction;
                hasVoucherTransaction |= childBundle.hasVoucherTransaction;
                hasIouTransaction |= childBundle.hasIouTransaction;
                hasAgentTransaction |= childBundle.hasAgentTransaction;
                hasChequeTransaction |= childBundle.hasChequeTransaction;
                hasSlipTransaction |= childBundle.hasSlipTransaction;
                hasEWalletTransaction |= childBundle.hasEWalletTransaction;
                hasPatientDepositTransaction |= childBundle.hasPatientDepositTransaction;
                hasPatientPointsTransaction |= childBundle.hasPatientPointsTransaction;
                hasOnlineSettlementTransaction |= childBundle.hasOnlineSettlementTransaction;
            }
        }
    }

    public void prepareDenominations() {
        denominationTransactions = createDefaultDenominationTransaction(PaymentMethod.Cash);
    }

    public Double getPaymentValue(PaymentMethod pm) {
        switch (pm) {
            case Agent:
                return agentValue;
            case Card:
                return cardValue;
            case Cash:
//                if (denominationTransactions == null) {
//                    denominationTransactions = createDefaultDenominationTransaction(PaymentMethod.Cash);
//                }
                return cashValue;
            case Cheque:
                return chequeValue;
            case Credit:
                return creditValue;
            case IOU:
                return iouValue;
            case MultiplePaymentMethods:
                return multiplePaymentMethodsValue;
            case OnlineSettlement:
                return onlineSettlementValue;
            case PatientDeposit:
                return patientDepositValue;
            case PatientPoints:
                return patientPointsValue;
            case Slip:
                return slipValue;
            case Staff:
                return staffValue;
            case Staff_Welfare:
                return staffWelfareValue;
            case Voucher:
                return voucherValue;
            case YouOweMe:
                return iouValue; // Assuming YouOweMe is equivalent to IOU
            case ewallet:
                return eWalletValue;
            default:
                return null;
        }
    }

    public Double getPaymentHandoverValue(PaymentMethod pm) {
        switch (pm) {
            case Agent:
                return agentHandoverValue;
            case Card:
                return cardHandoverValue;
            case Cash:
                return cashHandoverValue;
//                return denominatorValue;
            case Cheque:
                return chequeHandoverValue;
            case Credit:
                return creditHandoverValue;
            case IOU:
                return iouHandoverValue;
            case MultiplePaymentMethods:
                return multiplePaymentMethodsHandoverValue;
            case OnlineSettlement:
                return onlineSettlementHandoverValue;
            case PatientDeposit:
                return patientDepositHandoverValue;
            case PatientPoints:
                return patientPointsHandoverValue;
            case Slip:
                return slipHandoverValue;
            case Staff:
                return staffHandoverValue;
            case Staff_Welfare:
                return staffWelfareHandoverValue;
            case Voucher:
                return voucherHandoverValue;
            case YouOweMe:
                return iouHandoverValue; // Assuming YouOweMe is equivalent to IOU
            case ewallet:
                return eWalletHandoverValue;
            default:
                return null;
        }
    }

    public List<ReportTemplateRow> getPaymentRows(PaymentMethod pm) {
        List<ReportTemplateRow> prows = new ArrayList<>();
        if (reportTemplateRows == null) {
            return prows;
        }
        if (reportTemplateRows.isEmpty()) {
            return prows;
        }
        for (ReportTemplateRow r : reportTemplateRows) {
            if (r.getPayment() == null) {
                continue;
            }
            if (r.getPayment().getPaymentMethod() == null) {
                continue;
            }
            if (r.getPayment().getPaymentMethod() == pm) {
                prows.add(r);
            }
        }
        return prows;
    }

    public ReportTemplateRowBundle(List<ReportTemplateRow> reportTemplateRows) {
        this.reportTemplateRows = reportTemplateRows;
    }

    public UUID getId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReportTemplateRowBundle that = (ReportTemplateRowBundle) o;
        return Objects.equals(getId(), that.id);
    }

    // Override hashCode() using UUID field
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    // Override toString() for better readability (optional)
    @Override
    public String toString() {
        return "ReportTemplateRowBundle{id=" + getId() + '}';
    }

    public void calculateTotalsForProfessionalFees() {
        System.out.println("calculateTotals = ");
        this.total = 0.0;
        this.totalIn = 0.0;
        this.totalOut = 0.0;
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getBillFee() == null) {
                    continue;
                }
//                if (row.getBillFee().getBill().isCancelled()) {
//                    continue;
//                }
//                if (row.getBillFee().getBillItem().isRefunded()) {
//                    continue;
//                }
//                //TODO: Remove
//                if (row.getBillFee().getBill().isRefunded()) {
//                    continue;
//                }
                this.total += row.getBillFee().getFeeValue();
                this.totalIn += row.getBillFee().getSettleValue();
                this.totalOut += row.getBillFee().getPaidValue();
            }
        }
    }

    public void calculateTotalByValues() {
        total = 0.0;
        grossTotal = 0.0;
        discount = 0.0;
        count = 0l;
        for (ReportTemplateRow r : getReportTemplateRows()) {
            grossTotal += r.getGrossTotal();
            discount += r.getDiscount();
            total += r.getTotal();
            count += r.getRowCount();
        }
    }

    public void calculateTotalsForAllPaymentMethods() {
        System.out.println("calculateTotals = ");
        resetTotalsAndFlags();

        // Check if the list of rows is not null and not empty
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            // Aggregate values from each row and update transaction flags
            for (ReportTemplateRow row : this.reportTemplateRows) {
                addValueAndUpdateFlag("cash", safeDouble(row.getCashValue()));
                addValueAndUpdateFlag("card", safeDouble(row.getCardValue()));
                addValueAndUpdateFlag("multiplePaymentMethods", safeDouble(row.getMultiplePaymentMethodsValue()));
                addValueAndUpdateFlag("staff", safeDouble(row.getStaffValue()));
                addValueAndUpdateFlag("credit", safeDouble(row.getCreditValue()));
                addValueAndUpdateFlag("staffWelfare", safeDouble(row.getStaffWelfareValue()));
                addValueAndUpdateFlag("voucher", safeDouble(row.getVoucherValue()));
                addValueAndUpdateFlag("iou", safeDouble(row.getIouValue()));
                addValueAndUpdateFlag("agent", safeDouble(row.getAgentValue()));
                addValueAndUpdateFlag("cheque", safeDouble(row.getChequeValue()));
                addValueAndUpdateFlag("slip", safeDouble(row.getSlipValue()));
                addValueAndUpdateFlag("eWallet", safeDouble(row.getEwalletValue()));
                addValueAndUpdateFlag("patientDeposit", safeDouble(row.getPatientDepositValue()));
                addValueAndUpdateFlag("patientPoints", safeDouble(row.getPatientPointsValue()));
                addValueAndUpdateFlag("onlineSettlement", safeDouble(row.getOnlineSettlementValue()));
                addValueAndUpdateFlag("grossTotal", safeDouble(row.getGrossTotal()));
                addValueAndUpdateFlag("discount", safeDouble(row.getDiscount()));
                addValueAndUpdateFlag("total", safeDouble(row.getTotal()));
                addValueAndUpdateFlag("hospitalTotal", safeDouble(row.getHospitalTotal()));
                addValueAndUpdateFlag("staffTotal", safeDouble(row.getStaffTotal()));
                addValueAndUpdateFlag("ccTotal", safeDouble(row.getCcTotal()));
            }
        }
        total
                = this.cashValue + this.cardValue + this.multiplePaymentMethodsValue + this.staffValue
                + this.creditValue + this.staffWelfareValue + this.voucherValue + this.iouValue
                + this.agentValue + this.chequeValue + this.slipValue + this.eWalletValue
                + this.patientDepositValue + this.patientPointsValue + this.onlineSettlementValue;

    }

    public void calculateTotals() {
        System.out.println("calculateTotals = ");
        resetTotalsAndFlags();

        // Check if the list of rows is not null and not empty
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            // Aggregate values from each row and update transaction flags
            for (ReportTemplateRow row : this.reportTemplateRows) {
                addValueAndUpdateFlag("cash", safeDouble(row.getCashValue()));
                addValueAndUpdateFlag("card", safeDouble(row.getCardValue()));
                addValueAndUpdateFlag("multiplePaymentMethods", safeDouble(row.getMultiplePaymentMethodsValue()));
                addValueAndUpdateFlag("staff", safeDouble(row.getStaffValue()));
                addValueAndUpdateFlag("credit", safeDouble(row.getCreditValue()));
                addValueAndUpdateFlag("staffWelfare", safeDouble(row.getStaffWelfareValue()));
                addValueAndUpdateFlag("voucher", safeDouble(row.getVoucherValue()));
                addValueAndUpdateFlag("iou", safeDouble(row.getIouValue()));
                addValueAndUpdateFlag("agent", safeDouble(row.getAgentValue()));
                addValueAndUpdateFlag("cheque", safeDouble(row.getChequeValue()));
                addValueAndUpdateFlag("slip", safeDouble(row.getSlipValue()));
                addValueAndUpdateFlag("eWallet", safeDouble(row.getEwalletValue()));
                addValueAndUpdateFlag("patientDeposit", safeDouble(row.getPatientDepositValue()));
                addValueAndUpdateFlag("patientPoints", safeDouble(row.getPatientPointsValue()));
                addValueAndUpdateFlag("onlineSettlement", safeDouble(row.getOnlineSettlementValue()));
                addValueAndUpdateFlag("grossTotal", safeDouble(row.getGrossTotal()));
                addValueAndUpdateFlag("discount", safeDouble(row.getDiscount()));
                addValueAndUpdateFlag("total", safeDouble(row.getTotal()));
                addValueAndUpdateFlag("hospitalTotal", safeDouble(row.getHospitalTotal()));
                addValueAndUpdateFlag("staffTotal", safeDouble(row.getStaffTotal()));
                addValueAndUpdateFlag("ccTotal", safeDouble(row.getCcTotal()));
            }
        }
        total
                = this.cashValue + this.cardValue + this.voucherValue + this.iouValue + this.patientDepositValue
                + this.chequeValue + this.slipValue + this.eWalletValue;

    }

    public void calculateTotalsWithCredit() {
        System.out.println("calculateTotals = ");
        resetTotalsAndFlags();

        // Check if the list of rows is not null and not empty
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            // Aggregate values from each row and update transaction flags
            for (ReportTemplateRow row : this.reportTemplateRows) {
                addValueAndUpdateFlag("cash", safeDouble(row.getCashValue()));
                addValueAndUpdateFlag("card", safeDouble(row.getCardValue()));
                addValueAndUpdateFlag("multiplePaymentMethods", safeDouble(row.getMultiplePaymentMethodsValue()));
                addValueAndUpdateFlag("staff", safeDouble(row.getStaffValue()));
                addValueAndUpdateFlag("credit", safeDouble(row.getCreditValue()));
                addValueAndUpdateFlag("staffWelfare", safeDouble(row.getStaffWelfareValue()));
                addValueAndUpdateFlag("voucher", safeDouble(row.getVoucherValue()));
                addValueAndUpdateFlag("iou", safeDouble(row.getIouValue()));
                addValueAndUpdateFlag("agent", safeDouble(row.getAgentValue()));
                addValueAndUpdateFlag("cheque", safeDouble(row.getChequeValue()));
                addValueAndUpdateFlag("slip", safeDouble(row.getSlipValue()));
                addValueAndUpdateFlag("eWallet", safeDouble(row.getEwalletValue()));
                addValueAndUpdateFlag("patientDeposit", safeDouble(row.getPatientDepositValue()));
                addValueAndUpdateFlag("patientPoints", safeDouble(row.getPatientPointsValue()));
                addValueAndUpdateFlag("onlineSettlement", safeDouble(row.getOnlineSettlementValue()));
                addValueAndUpdateFlag("grossTotal", safeDouble(row.getGrossTotal()));
                addValueAndUpdateFlag("discount", safeDouble(row.getDiscount()));
                addValueAndUpdateFlag("total", safeDouble(row.getTotal()));
                addValueAndUpdateFlag("hospitalTotal", safeDouble(row.getHospitalTotal()));
                addValueAndUpdateFlag("staffTotal", safeDouble(row.getStaffTotal()));
                addValueAndUpdateFlag("ccTotal", safeDouble(row.getCcTotal()));
            }
        }
        total
                = this.cashValue
                + this.cardValue
                + this.voucherValue
                + this.iouValue
                //                + this.patientDepositValue
                + this.chequeValue
                + this.slipValue
                + this.creditValue
                + this.eWalletValue;

    }

    public void calculateTotalsBySelectedChildBundles() {
        calculateTotalsByChildBundles(true);
    }

    public void calculateTotalsByAllChildBundles() {
        calculateTotalsByChildBundles(false);
    }

    public void calculateTotalsByChildBundles(boolean forHandover) {
        System.out.println("calculateTotalsByChildBundles");
        resetTotalsAndFlags();
        System.out.println("total = " + total);
        boolean selectAll = !forHandover;

        if (this.bundles != null && !this.bundles.isEmpty()) {
            for (ReportTemplateRowBundle childBundle : this.bundles) {

                if (childBundle.isSelected() || selectAll) {

                    if (forHandover) {
                        childBundle.calculateTotalsByPaymentsAndDenominationsForHandover();
                    }

                    System.out.println("selected childBundle = " + childBundle.getName());
                    addValueAndUpdateFlag("cash", safeDouble(childBundle.getCashValue()), safeDouble(childBundle.getCashHandoverValue()));
                    addValueAndUpdateFlag("card", safeDouble(childBundle.getCardValue()), safeDouble(childBundle.getCardHandoverValue()));
                    addValueAndUpdateFlag("multiplePaymentMethods", safeDouble(childBundle.getMultiplePaymentMethodsValue()), safeDouble(childBundle.getMultiplePaymentMethodsHandoverValue()));
                    addValueAndUpdateFlag("staff", safeDouble(childBundle.getStaffValue()), safeDouble(childBundle.getStaffHandoverValue()));
                    addValueAndUpdateFlag("credit", safeDouble(childBundle.getCreditValue()), safeDouble(childBundle.getCreditHandoverValue()));
                    addValueAndUpdateFlag("staffWelfare", safeDouble(childBundle.getStaffWelfareValue()), safeDouble(childBundle.getStaffWelfareHandoverValue()));
                    addValueAndUpdateFlag("voucher", safeDouble(childBundle.getVoucherValue()), safeDouble(childBundle.getVoucherHandoverValue()));
                    addValueAndUpdateFlag("iou", safeDouble(childBundle.getIouValue()), safeDouble(childBundle.getIouHandoverValue()));
                    addValueAndUpdateFlag("agent", safeDouble(childBundle.getAgentValue()), safeDouble(childBundle.getAgentHandoverValue()));
                    addValueAndUpdateFlag("cheque", safeDouble(childBundle.getChequeValue()), safeDouble(childBundle.getChequeHandoverValue()));
                    addValueAndUpdateFlag("slip", safeDouble(childBundle.getSlipValue()), safeDouble(childBundle.getSlipHandoverValue()));
                    addValueAndUpdateFlag("eWallet", safeDouble(childBundle.getEwalletValue()), safeDouble(childBundle.getEwalletHandoverValue()));
                    addValueAndUpdateFlag("patientDeposit", safeDouble(childBundle.getPatientDepositValue()), safeDouble(childBundle.getPatientDepositHandoverValue()));
                    addValueAndUpdateFlag("patientPoints", safeDouble(childBundle.getPatientPointsValue()), safeDouble(childBundle.getPatientPointsHandoverValue()));
                    addValueAndUpdateFlag("onlineSettlement", safeDouble(childBundle.getOnlineSettlementValue()), safeDouble(childBundle.getOnlineSettlementHandoverValue()));
                    addValueAndUpdateFlag("grossTotal", safeDouble(childBundle.getGrossTotal()));
                    addValueAndUpdateFlag("discount", safeDouble(childBundle.getDiscount()));

                    addValueAndUpdateFlag("hospitalTotal", safeDouble(childBundle.getHospitalTotal()));
                    addValueAndUpdateFlag("staffTotal", safeDouble(childBundle.getStaffTotal()));
                    addValueAndUpdateFlag("ccTotal", safeDouble(childBundle.getCcTotal()));

                    System.out.println("childBundle.getTotal() = " + childBundle.getTotal());

                    System.out.println("total Before= " + total);

                    addValueAndUpdateFlag("total", safeDouble(childBundle.getTotal()));

                    System.out.println("total After= " + total);
                }
            }
        }
    }

    public void calculateTotalsByChildBundlesForHandover() {
        System.out.println("calculateTotalsByChildBundlesForHandover");
        resetTotalsAndFlags();
        System.out.println("total = " + total);

        if (this.bundles != null && !this.bundles.isEmpty()) {
            for (ReportTemplateRowBundle childBundle : this.bundles) {

                if (childBundle.isSelected()) {

                    childBundle.calculateTotalsOfSelectedRowsPlusAllCash();

                    System.out.println("selected childBundle = " + childBundle.getName());
                    addValueAndUpdateFlag("cash", safeDouble(childBundle.getCashValue()), safeDouble(childBundle.getCashHandoverValue()));
                    addValueAndUpdateFlag("card", safeDouble(childBundle.getCardValue()), safeDouble(childBundle.getCardHandoverValue()));
                    addValueAndUpdateFlag("multiplePaymentMethods", safeDouble(childBundle.getMultiplePaymentMethodsValue()), safeDouble(childBundle.getMultiplePaymentMethodsHandoverValue()));
                    addValueAndUpdateFlag("staff", safeDouble(childBundle.getStaffValue()), safeDouble(childBundle.getStaffHandoverValue()));
                    addValueAndUpdateFlag("credit", safeDouble(childBundle.getCreditValue()), safeDouble(childBundle.getCreditHandoverValue()));
                    addValueAndUpdateFlag("staffWelfare", safeDouble(childBundle.getStaffWelfareValue()), safeDouble(childBundle.getStaffWelfareHandoverValue()));
                    addValueAndUpdateFlag("voucher", safeDouble(childBundle.getVoucherValue()), safeDouble(childBundle.getVoucherHandoverValue()));
                    addValueAndUpdateFlag("iou", safeDouble(childBundle.getIouValue()), safeDouble(childBundle.getIouHandoverValue()));
                    addValueAndUpdateFlag("agent", safeDouble(childBundle.getAgentValue()), safeDouble(childBundle.getAgentHandoverValue()));
                    addValueAndUpdateFlag("cheque", safeDouble(childBundle.getChequeValue()), safeDouble(childBundle.getChequeHandoverValue()));
                    addValueAndUpdateFlag("slip", safeDouble(childBundle.getSlipValue()), safeDouble(childBundle.getSlipHandoverValue()));
                    addValueAndUpdateFlag("eWallet", safeDouble(childBundle.getEwalletValue()), safeDouble(childBundle.getEwalletHandoverValue()));
                    addValueAndUpdateFlag("patientDeposit", safeDouble(childBundle.getPatientDepositValue()), safeDouble(childBundle.getPatientDepositHandoverValue()));
                    addValueAndUpdateFlag("patientPoints", safeDouble(childBundle.getPatientPointsValue()), safeDouble(childBundle.getPatientPointsHandoverValue()));
                    addValueAndUpdateFlag("onlineSettlement", safeDouble(childBundle.getOnlineSettlementValue()), safeDouble(childBundle.getOnlineSettlementHandoverValue()));
                    addValueAndUpdateFlag("grossTotal", safeDouble(childBundle.getGrossTotal()));
                    addValueAndUpdateFlag("discount", safeDouble(childBundle.getDiscount()));

                    System.out.println("childBundle.getTotal() = " + childBundle.getTotal());
                    System.out.println("total Before= " + total);
                    addValueAndUpdateFlag("total", safeDouble(childBundle.getTotal()));
                    System.out.println("total After= " + total);

                    System.out.println("childBundle.totalOut() = " + childBundle.getTotalOut());
                    System.out.println("totalOut Before= " + totalOut);
                    addValueAndUpdateFlag("totalOut", safeDouble(childBundle.getTotalOut()));
                    System.out.println("totalOut After= " + totalOut);

                }
            }
        }

        calculateTotalHandoverByDenominationQuantities();
        cashHandoverValue = denominatorValue;
    }

    public void calculateTotalsBySelectedPayments() {
        resetTotalsAndFlags();

        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }

                Double amount = safeDouble(row.getPayment().getPaidValue());  // Ensure amounts are not null
                Double amountHandingOver = 0.0;

                if (row.isSelected()) {
                    amountHandingOver = amount;
                }

                PaymentMethod method = row.getPayment().getPaymentMethod();  // Using the enum type directly
                total += amount;

                switch (method) {
                    case Agent:
                        this.agentValue += amount;
                        this.agentHandoverValue += amountHandingOver;
                        this.hasAgentTransaction = true;
                        break;
                    case Card:
                        this.cardValue += amount;
                        this.cardHandoverValue += amountHandingOver;
                        this.hasCardTransaction = true;
                        break;
                    case Cash:
//                        if (denominationTransactions == null) {
//                            denominationTransactions = createDefaultDenominationTransaction(PaymentMethod.Cash);
//                        }
                        this.cashValue += amount;
                        this.cashHandoverValue += amountHandingOver;
                        this.hasCashTransaction = true;
                        break;
                    case Cheque:
                        this.chequeValue += amount;
                        this.chequeHandoverValue += amountHandingOver;
                        this.hasChequeTransaction = true;
                        break;
                    case Credit:
                        this.creditValue += amount;
                        this.creditHandoverValue += amountHandingOver;
                        this.hasCreditTransaction = true;
                        break;
                    case IOU:
                        this.iouValue += amount;
                        this.iouHandoverValue += amountHandingOver;
                        this.hasIouTransaction = true;
                        break;
                    case MultiplePaymentMethods:
                        this.multiplePaymentMethodsValue += amount;
                        this.multiplePaymentMethodsHandoverValue += amountHandingOver;
                        this.hasMultiplePaymentMethodsTransaction = true;
                        break;
                    case OnlineSettlement:
                        this.onlineSettlementValue += amount;
                        this.onlineSettlementHandoverValue += amountHandingOver;
                        this.hasOnlineSettlementTransaction = true;
                        break;
                    case PatientDeposit:
                        this.patientDepositValue += amount;
                        this.patientDepositHandoverValue += amountHandingOver;
                        this.hasPatientDepositTransaction = true;
                        break;
                    case PatientPoints:
                        this.patientPointsValue += amount;
                        this.patientPointsHandoverValue += amountHandingOver;
                        this.hasPatientPointsTransaction = true;
                        break;
                    case Slip:
                        this.slipValue += amount;
                        this.slipHandoverValue += amountHandingOver;
                        this.hasSlipTransaction = true;
                        break;
                    case Staff:
                        this.staffValue += amount;
                        this.staffHandoverValue += amountHandingOver;
                        this.hasStaffTransaction = true;
                        break;
                    case Staff_Welfare:
                        this.staffWelfareValue += amount;
                        this.staffWelfareHandoverValue += amountHandingOver;
                        this.hasStaffWelfareTransaction = true;
                        break;
                    case Voucher:
                        this.voucherValue += amount;
                        this.voucherHandoverValue += amountHandingOver;
                        this.hasVoucherTransaction = true;
                        break;
                    case YouOweMe:
                        this.iouValue += amount;  // Assuming YouOweMe is equivalent to IOU
                        this.iouHandoverValue += amountHandingOver;
                        this.hasIouTransaction = true;
                        break;
                    case ewallet:
                        this.eWalletValue += amount;
                        this.eWalletHandoverValue += amountHandingOver;
                        this.hasEWalletTransaction = true;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void calculateTotalByBills() {
        total = 0.0;
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getBill() == null) {
                    continue;
                }
                Double amount = safeDouble(row.getBill().getNetTotal());
                total += amount;
            }
        }
    }

    public void calculateTotalByHospitalFee() {
        total = 0.0;
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getBill() == null) {
                    continue;
                }
                Double amount = safeDouble(row.getBill().getTotalHospitalFee());
                total += amount;
            }
        }
    }

    public void calculateTotalCCFee() {
        ccTotal = 0.0;
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getBill() == null) {
                    continue;
                }
                Double amount = safeDouble(row.getBill().getCollctingCentreFee());
                ccTotal += amount;
            }
        }
    }

    public void calculateTotalByBillItems() {
        total = 0.0;

        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getBillItem() == null) {
                    continue;
                }
                Double amount = safeDouble(row.getBillItem().getBill().getNetTotal());
                total += amount;
            }
        }
    }

    public void calculateTotalByRowTotals() {
        total = 0.0;        // Ensure all counters are reset before starting calculations
        grossTotal = 0.0;
        discount = 0.0;
        tax = 0.0;
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row != null) {
                    Double iteratingTotal = safeDouble(row.getGrossTotal());
                    grossTotal += iteratingTotal;

                    Double iteratingDiscount = safeDouble(row.getDiscount());
                    discount += iteratingDiscount;

                    Double iteratingTax = safeDouble(row.getTax());
                    tax += iteratingTax;

                    Double iteratingNetTotal = safeDouble(row.getTotal()); // assuming you meant to use getTotal here as well for the net total calculation
                    total += iteratingNetTotal;
                }
            }
        }
    }

    public void calculateTotalNetTotalTaxByBills() {
        total = 0.0;
        grossTotal = 0.0;
        tax = 0.0;
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getBill() == null) {
                    continue;
                }
                Double billTotal = safeDouble(row.getBill().getNetTotal());
                Double billGrossTotal = safeDouble(row.getBill().getTotal());
                Double billTax = safeDouble(row.getBill().getTax());
                total += billTotal;
                grossTotal += billGrossTotal;
                tax += billTax;
            }
        }
    }

    public void calculateTotalByPayments() {
        total = 0.0;
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null) {
                    continue;
                }
                Double amount = safeDouble(row.getPayment().getPaidValue());
                total += amount;
            }
        }
    }

    public void markAllAtHandover(PaymentMethod inputPaymentMethod) {
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }
                if (row.getPayment().getPaymentMethod() == inputPaymentMethod) {
                    row.getPayment().setSelectedForHandover(true);
                    row.setSelected(row.getPayment().isSelectedForHandover());
                }
            }
        }
    }

    public void unmarkAllAtHandover() {
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() != null && row.getPayment().isSelectedForHandover()) {
                    row.getPayment().setSelectedForHandover(false);
                    row.setSelected(false);
                }
            }
        }
    }

    public void markSelectedAtHandover() {
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }
                row.setSelected(row.getPayment().isSelectedForHandover());
            }
        }
    }

    public void markSelectedAtHandoverAccept() {
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }
                row.setSelected(row.getPayment().isSelectedForCashbookEntry());
            }
        }
    }

    public void markSelectedAtRecordCreation() {
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }
                row.setSelected(row.getPayment().isSelectedForRecording());
            }
        }
    }

    public void markSelectedAtRecordConfirmation() {
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }
                row.setSelected(row.getPayment().isSelectedForRecordingConfirmation());
            }
        }
    }

    public void calculateTotalsByPaymentsAndDenominations() {
        resetTotalsAndFlags();

        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }

                Double amount = safeDouble(row.getPayment().getPaidValue());  // Ensure amounts are not null
                Double amountHandingOver = 0.0;

                PaymentMethod method = row.getPayment().getPaymentMethod();

                if (row.isSelected()) {
                    amountHandingOver = amount;
                } else {
                    if (method == Cash && getSelectAllCashToHandover()) {
                        amountHandingOver = amount;
                    }
                }

                total += amount;

                switch (method) {
                    case Agent:
                        this.agentValue += amount;
                        this.agentHandoverValue += amountHandingOver;
                        this.hasAgentTransaction = true;
                        break;
                    case Card:
                        this.cardValue += amount;
                        this.cardHandoverValue += amountHandingOver;
                        this.hasCardTransaction = true;
                        break;
                    case Cash:
                        this.cashValue += amount;
                        this.cashHandoverValue += amountHandingOver;
                        this.hasCashTransaction = true;
                        break;
                    case Cheque:
                        this.chequeValue += amount;
                        this.chequeHandoverValue += amountHandingOver;
                        this.hasChequeTransaction = true;
                        break;
                    case Credit:
                        this.creditValue += amount;
                        this.creditHandoverValue += amountHandingOver;
                        this.hasCreditTransaction = true;
                        break;
                    case IOU:
                        this.iouValue += amount;
                        this.iouHandoverValue += amountHandingOver;
                        this.hasIouTransaction = true;
                        break;
                    case MultiplePaymentMethods:
                        this.multiplePaymentMethodsValue += amount;
                        this.multiplePaymentMethodsHandoverValue += amountHandingOver;
                        this.hasMultiplePaymentMethodsTransaction = true;
                        break;
                    case OnlineSettlement:
                        this.onlineSettlementValue += amount;
                        this.onlineSettlementHandoverValue += amountHandingOver;
                        this.hasOnlineSettlementTransaction = true;
                        break;
                    case PatientDeposit:
                        this.patientDepositValue += amount;
                        this.patientDepositHandoverValue += amountHandingOver;
                        this.hasPatientDepositTransaction = true;
                        break;
                    case PatientPoints:
                        this.patientPointsValue += amount;
                        this.patientPointsHandoverValue += amountHandingOver;
                        this.hasPatientPointsTransaction = true;
                        break;
                    case Slip:
                        this.slipValue += amount;
                        this.slipHandoverValue += amountHandingOver;
                        this.hasSlipTransaction = true;
                        break;
                    case Staff:
                        this.staffValue += amount;
                        this.staffHandoverValue += amountHandingOver;
                        this.hasStaffTransaction = true;
                        break;
                    case Staff_Welfare:
                        this.staffWelfareValue += amount;
                        this.staffWelfareHandoverValue += amountHandingOver;
                        this.hasStaffWelfareTransaction = true;
                        break;
                    case Voucher:
                        this.voucherValue += amount;
                        this.voucherHandoverValue += amountHandingOver;
                        this.hasVoucherTransaction = true;
                        break;
                    case YouOweMe:
                        this.iouValue += amount;  // Assuming YouOweMe is equivalent to IOU
                        this.iouHandoverValue += amountHandingOver;
                        this.hasIouTransaction = true;
                        break;
                    case ewallet:
                        this.eWalletValue += amount;
                        this.eWalletHandoverValue += amountHandingOver;
                        this.hasEWalletTransaction = true;
                        break;
                    default:

                        break;
                }
            }
        }
        calculateTotalHandoverByDenominationQuantities();
    }

    public void calculateTotalsOfSelectedRowsPlusAllCash() {
        resetTotalsAndFlags();

        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }

                Double amount = safeDouble(row.getPayment().getPaidValue());  // Ensure amounts are not null
                Double amountHandingOver = 0.0;

                PaymentMethod method = row.getPayment().getPaymentMethod();

                if (row.isSelected()) {
                    amountHandingOver = amount;
                } else {
                    if (method == Cash && getSelectAllCashToHandover()) {
                        amountHandingOver = amount;
                    }
                }

                total += amount;
                totalOut += amountHandingOver;

                switch (method) {
                    case Agent:
                        this.agentValue += amount;
                        this.agentHandoverValue += amountHandingOver;
                        this.hasAgentTransaction = true;
                        break;
                    case Card:
                        this.cardValue += amount;
                        this.cardHandoverValue += amountHandingOver;
                        this.hasCardTransaction = true;
                        break;
                    case Cash:
                        this.cashValue += amount;
                        this.cashHandoverValue += amountHandingOver;
                        this.hasCashTransaction = true;
                        break;
                    case Cheque:
                        this.chequeValue += amount;
                        this.chequeHandoverValue += amountHandingOver;
                        this.hasChequeTransaction = true;
                        break;
                    case Credit:
                        this.creditValue += amount;
                        this.creditHandoverValue += amountHandingOver;
                        this.hasCreditTransaction = true;
                        break;
                    case IOU:
                        this.iouValue += amount;
                        this.iouHandoverValue += amountHandingOver;
                        this.hasIouTransaction = true;
                        break;
                    case MultiplePaymentMethods:
                        this.multiplePaymentMethodsValue += amount;
                        this.multiplePaymentMethodsHandoverValue += amountHandingOver;
                        this.hasMultiplePaymentMethodsTransaction = true;
                        break;
                    case OnlineSettlement:
                        this.onlineSettlementValue += amount;
                        this.onlineSettlementHandoverValue += amountHandingOver;
                        this.hasOnlineSettlementTransaction = true;
                        break;
                    case PatientDeposit:
                        this.patientDepositValue += amount;
                        this.patientDepositHandoverValue += amountHandingOver;
                        this.hasPatientDepositTransaction = true;
                        break;
                    case PatientPoints:
                        this.patientPointsValue += amount;
                        this.patientPointsHandoverValue += amountHandingOver;
                        this.hasPatientPointsTransaction = true;
                        break;
                    case Slip:
                        this.slipValue += amount;
                        this.slipHandoverValue += amountHandingOver;
                        this.hasSlipTransaction = true;
                        break;
                    case Staff:
                        this.staffValue += amount;
                        this.staffHandoverValue += amountHandingOver;
                        this.hasStaffTransaction = true;
                        break;
                    case Staff_Welfare:
                        this.staffWelfareValue += amount;
                        this.staffWelfareHandoverValue += amountHandingOver;
                        this.hasStaffWelfareTransaction = true;
                        break;
                    case Voucher:
                        this.voucherValue += amount;
                        this.voucherHandoverValue += amountHandingOver;
                        this.hasVoucherTransaction = true;
                        break;
                    case YouOweMe:
                        this.iouValue += amount;  // Assuming YouOweMe is equivalent to IOU
                        this.iouHandoverValue += amountHandingOver;
                        this.hasIouTransaction = true;
                        break;
                    case ewallet:
                        this.eWalletValue += amount;
                        this.eWalletHandoverValue += amountHandingOver;
                        this.hasEWalletTransaction = true;
                        break;
                    default:

                        break;
                }
            }
        }
    }

    public void calculateTotalsByPaymentsAndDenominationsForHandover() {
        resetTotalsAndFlags();

        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                if (row.getPayment() == null || row.getPayment().getPaymentMethod() == null) {
                    continue;
                }

                Double amount = safeDouble(row.getPayment().getPaidValue());  // Ensure amounts are not null
                Double amountHandingOver = 0.0;

                PaymentMethod method = row.getPayment().getPaymentMethod();
                if (method == Cash) {
                    if (selectAllCashToHandover) {
                        amountHandingOver = amount;
                    }
                } else {
                    if (row.getPayment().isSelectedForHandover()) {
                        amountHandingOver = amount;
                    }
                }

                total += amount;

                switch (method) {
                    case Agent:
                        this.agentValue += amount;
                        this.agentHandoverValue += amountHandingOver;
                        this.hasAgentTransaction = true;
                        break;
                    case Card:
                        this.cardValue += amount;
                        this.cardHandoverValue += amountHandingOver;
                        this.hasCardTransaction = true;
                        break;
                    case Cash:
                        this.cashValue += amount;
                        this.cashHandoverValue += amountHandingOver;
                        this.hasCashTransaction = true;
                        break;
                    case Cheque:
                        this.chequeValue += amount;
                        this.chequeHandoverValue += amountHandingOver;
                        this.hasChequeTransaction = true;
                        break;
                    case Credit:
                        this.creditValue += amount;
                        this.creditHandoverValue += amountHandingOver;
                        this.hasCreditTransaction = true;
                        break;
                    case IOU:
                        this.iouValue += amount;
                        this.iouHandoverValue += amountHandingOver;
                        this.hasIouTransaction = true;
                        break;
                    case MultiplePaymentMethods:
                        this.multiplePaymentMethodsValue += amount;
                        this.multiplePaymentMethodsHandoverValue += amountHandingOver;
                        this.hasMultiplePaymentMethodsTransaction = true;
                        break;
                    case OnlineSettlement:
                        this.onlineSettlementValue += amount;
                        this.onlineSettlementHandoverValue += amountHandingOver;
                        this.hasOnlineSettlementTransaction = true;
                        break;
                    case PatientDeposit:
                        this.patientDepositValue += amount;
                        this.patientDepositHandoverValue += amountHandingOver;
                        this.hasPatientDepositTransaction = true;
                        break;
                    case PatientPoints:
                        this.patientPointsValue += amount;
                        this.patientPointsHandoverValue += amountHandingOver;
                        this.hasPatientPointsTransaction = true;
                        break;
                    case Slip:
                        this.slipValue += amount;
                        this.slipHandoverValue += amountHandingOver;
                        this.hasSlipTransaction = true;
                        break;
                    case Staff:
                        this.staffValue += amount;
                        this.staffHandoverValue += amountHandingOver;
                        this.hasStaffTransaction = true;
                        break;
                    case Staff_Welfare:
                        this.staffWelfareValue += amount;
                        this.staffWelfareHandoverValue += amountHandingOver;
                        this.hasStaffWelfareTransaction = true;
                        break;
                    case Voucher:
                        this.voucherValue += amount;
                        this.voucherHandoverValue += amountHandingOver;
                        this.hasVoucherTransaction = true;
                        break;
                    case YouOweMe:
                        this.iouValue += amount;  // Assuming YouOweMe is equivalent to IOU
                        this.iouHandoverValue += amountHandingOver;
                        this.hasIouTransaction = true;
                        break;
                    case ewallet:
                        this.eWalletValue += amount;
                        this.eWalletHandoverValue += amountHandingOver;
                        this.hasEWalletTransaction = true;
                        break;
                    default:

                        break;
                }
            }
        }
        calculateTotalHandoverByDenominationQuantities();
    }

    public void createRowValuesFromBill() {
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                System.out.println("Processing row: " + row);
                System.out.println("row.getBill() = " + row.getBill());

                if (row.getBill() == null) {
                    System.out.println("Skipping row, as bill is null.");
                    continue;
                }

                // Debugging bill details
                System.out.println("row.getBill().getGrantTotal() = " + row.getBill().getGrantTotal());
                System.out.println("row.getBill().getDiscount() = " + row.getBill().getDiscount());
                System.out.println("row.getBill().getNetTotal() = " + row.getBill().getNetTotal());
                System.out.println("row.getBill().getTotalStaffFee() = " + row.getBill().getTotalStaffFee());
                System.out.println("row.getBill().getTotalCenterFee() = " + row.getBill().getTotalCenterFee());
                System.out.println("row.getHospitalTotal() = " + row.getHospitalTotal());

                // Setting values
                row.setGrossTotal(row.getBill().getGrantTotal());
                row.setDiscount(row.getBill().getDiscount());
                row.setTotal(row.getBill().getNetTotal());
                row.setHospitalTotal(row.getHospitalTotal());
                row.setStaffTotal(row.getBill().getTotalStaffFee());
                row.setCcTotal(row.getBill().getTotalCenterFee());

                // Debugging after setting
                System.out.println("row.getGrossTotal() = " + row.getGrossTotal());
                System.out.println("row.getDiscount() = " + row.getDiscount());
                System.out.println("row.getTotal() = " + row.getTotal());
                System.out.println("row.getHospitalTotal() = " + row.getHospitalTotal());
                System.out.println("row.getStaffTotal() = " + row.getStaffTotal());
                System.out.println("row.getCcTotal() = " + row.getCcTotal());
            }
        } else {
            System.out.println("No reportTemplateRows to process.");
        }
    }

    public void createRowValuesFromBillItems() {
        if (this.reportTemplateRows != null && !this.reportTemplateRows.isEmpty()) {
            for (ReportTemplateRow row : this.reportTemplateRows) {
                System.out.println("Processing row: " + row);
                System.out.println("row.getBill() = " + row.getBill());

                if (row.getBillItem() == null) {
                    System.out.println("Skipping row, as bill is null.");
                    continue;
                }

                // Setting values
                row.setGrossTotal(row.getBillItem().getGrossValue());
                row.setDiscount(row.getBillItem().getDiscount());
                row.setTotal(row.getBillItem().getNetValue());
                row.setHospitalTotal(row.getHospitalTotal());
                row.setStaffTotal(row.getBillItem().getStaffFee());
                row.setCcTotal(row.getBillItem().getCollectingCentreFee());

                // Debugging after setting
                System.out.println("row.getGrossTotal() = " + row.getGrossTotal());
                System.out.println("row.getDiscount() = " + row.getDiscount());
                System.out.println("row.getTotal() = " + row.getTotal());
                System.out.println("row.getHospitalTotal() = " + row.getHospitalTotal());
                System.out.println("row.getStaffTotal() = " + row.getStaffTotal());
                System.out.println("row.getCcTotal() = " + row.getCcTotal());
            }
        } else {
            System.out.println("No reportTemplateRows to process.");
        }
    }

    private void resetTotalsAndFlags() {
        this.cashValue = this.cardValue = this.multiplePaymentMethodsValue = this.staffValue
                = this.creditValue = this.staffWelfareValue = this.voucherValue = this.iouValue
                = this.agentValue = this.chequeValue = this.slipValue = this.eWalletValue
                = this.patientDepositValue = this.patientPointsValue = this.onlineSettlementValue
                = this.grossTotal = this.discount = this.total = this.totalIn = this.totalOut
                = this.hospitalTotal = this.staffTotal = this.ccTotal = 0.0;

        // Reset handover values
        this.cashHandoverValue = this.cardHandoverValue = this.multiplePaymentMethodsHandoverValue = this.staffHandoverValue
                = this.creditHandoverValue = this.staffWelfareHandoverValue = this.voucherHandoverValue = this.iouHandoverValue
                = this.agentHandoverValue = this.chequeHandoverValue = this.slipHandoverValue = this.eWalletHandoverValue
                = this.patientDepositHandoverValue = this.patientPointsHandoverValue = this.onlineSettlementHandoverValue
                = this.onCallHandoverValue = 0.0;

        this.hasCashTransaction = this.hasCardTransaction = this.hasMultiplePaymentMethodsTransaction = this.hasStaffTransaction
                = this.hasCreditTransaction = this.hasStaffWelfareTransaction = this.hasVoucherTransaction = this.hasIouTransaction
                = this.hasAgentTransaction = this.hasChequeTransaction = this.hasSlipTransaction = this.hasEWalletTransaction
                = this.hasPatientDepositTransaction = this.hasPatientPointsTransaction = this.hasOnlineSettlementTransaction = false;
    }

    private void addValueAndUpdateFlag(String calculationAttribute, double amount) {
        if (amount != 0) {
            switch (calculationAttribute) {
                case "cash":
                    this.cashValue += amount;
                    this.hasCashTransaction = true;
                    break;
                case "card":
                    this.cardValue += amount;
                    this.hasCardTransaction = true;
                    break;
                case "multiplePaymentMethods":
                    this.multiplePaymentMethodsValue += amount;
                    this.hasMultiplePaymentMethodsTransaction = true;
                    break;
                case "staff":
                    this.staffValue += amount;
                    this.hasStaffTransaction = true;
                    break;
                case "credit":
                    this.creditValue += amount;
                    this.hasCreditTransaction = true;
                    break;
                case "staffWelfare":
                    this.staffWelfareValue += amount;
                    this.hasStaffWelfareTransaction = true;
                    break;
                case "voucher":
                    this.voucherValue += amount;
                    this.hasVoucherTransaction = true;
                    break;
                case "iou":
                    this.iouValue += amount;
                    this.hasIouTransaction = true;
                    break;
                case "agent":
                    this.agentValue += amount;
                    this.hasAgentTransaction = true;
                    break;
                case "cheque":
                    this.chequeValue += amount;
                    this.hasChequeTransaction = true;
                    break;
                case "slip":
                    this.slipValue += amount;
                    this.hasSlipTransaction = true;
                    break;
                case "eWallet":
                    this.eWalletValue += amount;
                    this.hasEWalletTransaction = true;
                    break;
                case "patientDeposit":
                    this.patientDepositValue += amount;
                    this.hasPatientDepositTransaction = true;
                    break;
                case "patientPoints":
                    this.patientPointsValue += amount;
                    this.hasPatientPointsTransaction = true;
                    break;
                case "onlineSettlement":
                    this.onlineSettlementValue += amount;
                    this.hasOnlineSettlementTransaction = true;
                    break;
                case "grossTotal":
                    this.grossTotal += amount;
                    break;
                case "discount":
                    this.discount += amount;
                    break;
                case "total":
                    this.total += amount;
                    break;
                case "totalOut":
                    this.totalOut += amount;
                    break;
                case "totalIn":
                    this.totalIn += amount;
                    break;
                case "hospitalTotal":
                    this.hospitalTotal += amount;
                    break;
                case "staffTotal":
                    this.staffTotal += amount;
                    break;
                case "ccTotal":
                    this.ccTotal += amount;
                    break;
                default:

                    break;
            }
        }
    }

    private void addValueAndUpdateFlag(String calculationAttribute, double amount, double handoverValue) {
        if (amount != 0) {
            switch (calculationAttribute) {
                case "cash":
                    this.cashValue += amount;
                    this.cashHandoverValue += handoverValue;
                    this.hasCashTransaction = true;
                    break;
                case "card":
                    this.cardValue += amount;
                    this.cardHandoverValue += handoverValue;
                    this.hasCardTransaction = true;
                    break;
                case "multiplePaymentMethods":
                    this.multiplePaymentMethodsValue += amount;
                    this.multiplePaymentMethodsHandoverValue += handoverValue;
                    this.hasMultiplePaymentMethodsTransaction = true;
                    break;
                case "staff":
                    this.staffValue += amount;
                    this.staffHandoverValue += handoverValue;
                    this.hasStaffTransaction = true;
                    break;
                case "credit":
                    this.creditValue += amount;
                    this.creditHandoverValue += handoverValue;
                    this.hasCreditTransaction = true;
                    break;
                case "staffWelfare":
                    this.staffWelfareValue += amount;
                    this.staffWelfareHandoverValue += handoverValue;
                    this.hasStaffWelfareTransaction = true;
                    break;
                case "voucher":
                    this.voucherValue += amount;
                    this.voucherHandoverValue += handoverValue;
                    this.hasVoucherTransaction = true;
                    break;
                case "iou":
                    this.iouValue += amount;
                    this.iouHandoverValue += handoverValue;
                    this.hasIouTransaction = true;
                    break;
                case "agent":
                    this.agentValue += amount;
                    this.agentHandoverValue += handoverValue;
                    this.hasAgentTransaction = true;
                    break;
                case "cheque":
                    this.chequeValue += amount;
                    this.chequeHandoverValue += handoverValue;
                    this.hasChequeTransaction = true;
                    break;
                case "slip":
                    this.slipValue += amount;
                    this.slipHandoverValue += handoverValue;
                    this.hasSlipTransaction = true;
                    break;
                case "eWallet":
                    this.eWalletValue += amount;
                    this.eWalletHandoverValue += handoverValue;
                    this.hasEWalletTransaction = true;
                    break;
                case "patientDeposit":
                    this.patientDepositValue += amount;
                    this.patientDepositHandoverValue += handoverValue;
                    this.hasPatientDepositTransaction = true;
                    break;
                case "patientPoints":
                    this.patientPointsValue += amount;
                    this.patientPointsHandoverValue += handoverValue;
                    this.hasPatientPointsTransaction = true;
                    break;
                case "onlineSettlement":
                    this.onlineSettlementValue += amount;
                    this.onlineSettlementHandoverValue += handoverValue;
                    this.hasOnlineSettlementTransaction = true;
                    break;
                case "grossTotal":
                    this.grossTotal += amount;
                    break;
                case "discount":
                    this.discount += amount;
                    break;
                case "total":
                    this.total += amount;
                    break;
                case "hospitalTotal":
                    this.hospitalTotal += amount;
                    break;
                case "staffTotal":
                    this.staffTotal += amount;
                    break;
                case "ccTotal":
                    this.ccTotal += amount;
                    break;
                default:
                    // No action for unknown attributes
                    break;
            }
        }
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    public double getOnCallValue() {
        return onCallValue;
    }

    public void setOnCallValue(double onCallValue) {
        this.onCallValue = onCallValue;
    }

    public double getCashValue() {
        return cashValue;
    }

    public void setCashValue(double cashValue) {
        this.cashValue = cashValue;
    }

    public double getCardValue() {
        return cardValue;
    }

    public void setCardValue(double cardValue) {
        this.cardValue = cardValue;
    }

    public double getMultiplePaymentMethodsValue() {
        return multiplePaymentMethodsValue;
    }

    public void setMultiplePaymentMethodsValue(double multiplePaymentMethodsValue) {
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
    }

    public double getStaffValue() {
        return staffValue;
    }

    public void setStaffValue(double staffValue) {
        this.staffValue = staffValue;
    }

    public double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(double creditValue) {
        this.creditValue = creditValue;
    }

    public double getStaffWelfareValue() {
        return staffWelfareValue;
    }

    public void setStaffWelfareValue(double staffWelfareValue) {
        this.staffWelfareValue = staffWelfareValue;
    }

    public double getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(double voucherValue) {
        this.voucherValue = voucherValue;
    }

    public double getIouValue() {
        return iouValue;
    }

    public void setIouValue(double iouValue) {
        this.iouValue = iouValue;
    }

    public double getAgentValue() {
        return agentValue;
    }

    public void setAgentValue(double agentValue) {
        this.agentValue = agentValue;
    }

    public double getChequeValue() {
        return chequeValue;
    }

    public void setChequeValue(double chequeValue) {
        this.chequeValue = chequeValue;
    }

    public double getSlipValue() {
        return slipValue;
    }

    public void setSlipValue(double slipValue) {
        this.slipValue = slipValue;
    }

    public double getEwalletValue() {
        return eWalletValue;
    }

    public void setEwalletValue(double eWalletValue) {
        this.eWalletValue = eWalletValue;
    }

    public double getPatientDepositValue() {
        return patientDepositValue;
    }

    public void setPatientDepositValue(double patientDepositValue) {
        this.patientDepositValue = patientDepositValue;
    }

    public double getPatientPointsValue() {
        return patientPointsValue;
    }

    public void setPatientPointsValue(double patientPointsValue) {
        this.patientPointsValue = patientPointsValue;
    }

    public double getOnlineSettlementValue() {
        return onlineSettlementValue;
    }

    public void setOnlineSettlementValue(double onlineSettlementValue) {
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public ReportTemplate getReportTemplate() {
        return reportTemplate;
    }

    public void setReportTemplate(ReportTemplate reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    public List<ReportTemplateRow> getReportTemplateRows() {
        if (reportTemplateRows == null) {
            reportTemplateRows = new ArrayList<>();
        }
        return reportTemplateRows;
    }

    public void setReportTemplateRows(List<ReportTemplateRow> reportTemplateRows) {
        this.reportTemplateRows = reportTemplateRows;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getTotalIn() {
        return totalIn;
    }

    public void setTotalIn(Double totalIn) {
        this.totalIn = totalIn;
    }

    public Double getTotalOut() {
        return totalOut;
    }

    public void setTotalOut(Double totalOut) {
        this.totalOut = totalOut;
    }

    public Long getCountIn() {
        return countIn;
    }

    public void setCountIn(Long countIn) {
        this.countIn = countIn;
    }

    public Long getCountOut() {
        return countOut;
    }

    public void setCountOut(Long countOut) {
        this.countOut = countOut;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getName() {
        if (name == null || name.isEmpty()) {
            name = "BundleName" + UUID.randomUUID().toString();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    public Long getLong1() {
        return long1;
    }

    public void setLong1(Long long1) {
        this.long1 = long1;
    }

    public Long getLong2() {
        return long2;
    }

    public void setLong2(Long long2) {
        this.long2 = long2;
    }

    public Long getLong3() {
        return long3;
    }

    public void setLong3(Long long3) {
        this.long3 = long3;
    }

    public Long getLong4() {
        return long4;
    }

    public void setLong4(Long long4) {
        this.long4 = long4;
    }

    public Long getLong5() {
        return long5;
    }

    public void setLong5(Long long5) {
        this.long5 = long5;
    }

    public Long getLong6() {
        return long6;
    }

    public void setLong6(Long long6) {
        this.long6 = long6;
    }

    public Long getLong7() {
        return long7;
    }

    public void setLong7(Long long7) {
        this.long7 = long7;
    }

    public Long getLong8() {
        return long8;
    }

    public void setLong8(Long long8) {
        this.long8 = long8;
    }

    public Long getLong9() {
        return long9;
    }

    public void setLong9(Long long9) {
        this.long9 = long9;
    }

    public Long getLong10() {
        return long10;
    }

    public void setLong10(Long long10) {
        this.long10 = long10;
    }

    public double geteWalletValue() {
        return eWalletValue;
    }

    public void seteWalletValue(double eWalletValue) {
        this.eWalletValue = eWalletValue;
    }

    public boolean isHasOnCallTransaction() {
        return hasOnCallTransaction;
    }

    public void setHasOnCallTransaction(boolean hasOnCallTransaction) {
        this.hasOnCallTransaction = hasOnCallTransaction;
    }

    public boolean isHasCashTransaction() {
        return hasCashTransaction;
    }

    public void setHasCashTransaction(boolean hasCashTransaction) {
        this.hasCashTransaction = hasCashTransaction;
    }

    public boolean isHasCardTransaction() {
        return hasCardTransaction;
    }

    public void setHasCardTransaction(boolean hasCardTransaction) {
        this.hasCardTransaction = hasCardTransaction;
    }

    public boolean isHasMultiplePaymentMethodsTransaction() {
        return hasMultiplePaymentMethodsTransaction;
    }

    public void setHasMultiplePaymentMethodsTransaction(boolean hasMultiplePaymentMethodsTransaction) {
        this.hasMultiplePaymentMethodsTransaction = hasMultiplePaymentMethodsTransaction;
    }

    public boolean isHasStaffTransaction() {
        return hasStaffTransaction;
    }

    public void setHasStaffTransaction(boolean hasStaffTransaction) {
        this.hasStaffTransaction = hasStaffTransaction;
    }

    public boolean isHasCreditTransaction() {
        return hasCreditTransaction;
    }

    public void setHasCreditTransaction(boolean hasCreditTransaction) {
        this.hasCreditTransaction = hasCreditTransaction;
    }

    public boolean isHasStaffWelfareTransaction() {
        return hasStaffWelfareTransaction;
    }

    public void setHasStaffWelfareTransaction(boolean hasStaffWelfareTransaction) {
        this.hasStaffWelfareTransaction = hasStaffWelfareTransaction;
    }

    public boolean isHasVoucherTransaction() {
        return hasVoucherTransaction;
    }

    public void setHasVoucherTransaction(boolean hasVoucherTransaction) {
        this.hasVoucherTransaction = hasVoucherTransaction;
    }

    public boolean isHasIouTransaction() {
        return hasIouTransaction;
    }

    public void setHasIouTransaction(boolean hasIouTransaction) {
        this.hasIouTransaction = hasIouTransaction;
    }

    public boolean isHasAgentTransaction() {
        return hasAgentTransaction;
    }

    public void setHasAgentTransaction(boolean hasAgentTransaction) {
        this.hasAgentTransaction = hasAgentTransaction;
    }

    public boolean isHasChequeTransaction() {
        return hasChequeTransaction;
    }

    public void setHasChequeTransaction(boolean hasChequeTransaction) {
        this.hasChequeTransaction = hasChequeTransaction;
    }

    public boolean isHasSlipTransaction() {
        return hasSlipTransaction;
    }

    public void setHasSlipTransaction(boolean hasSlipTransaction) {
        this.hasSlipTransaction = hasSlipTransaction;
    }

    public boolean isHasEWalletTransaction() {
        return hasEWalletTransaction;
    }

    public void setHasEWalletTransaction(boolean hasEWalletTransaction) {
        this.hasEWalletTransaction = hasEWalletTransaction;
    }

    public boolean isHasPatientDepositTransaction() {
        return hasPatientDepositTransaction;
    }

    public void setHasPatientDepositTransaction(boolean hasPatientDepositTransaction) {
        this.hasPatientDepositTransaction = hasPatientDepositTransaction;
    }

    public boolean isHasPatientPointsTransaction() {
        return hasPatientPointsTransaction;
    }

    public void setHasPatientPointsTransaction(boolean hasPatientPointsTransaction) {
        this.hasPatientPointsTransaction = hasPatientPointsTransaction;
    }

    public boolean isHasOnlineSettlementTransaction() {
        return hasOnlineSettlementTransaction;
    }

    public void setHasOnlineSettlementTransaction(boolean hasOnlineSettlementTransaction) {
        this.hasOnlineSettlementTransaction = hasOnlineSettlementTransaction;
    }

    public String getBundleType() {
        return bundleType;
    }

    public void setBundleType(String bundleType) {
        this.bundleType = bundleType;
    }

    public List<ReportTemplateRowBundle> getBundles() {
        if (bundles == null) {
            bundles = new ArrayList<>();
        }
        return bundles;
    }

    public void sortByDateInstitutionSiteDepartmentType() {
        if (bundles == null || bundles.isEmpty()) {
            return;
        }

        Collections.sort(bundles, new Comparator<ReportTemplateRowBundle>() {
            @Override
            public int compare(ReportTemplateRowBundle b1, ReportTemplateRowBundle b2) {
                // Compare by Date
                int dateCompare = b1.getDate().compareTo(b2.getDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }

                // Compare by Institution Name
                String institution1 = b1.getDepartment().getInstitution().getName();
                String institution2 = b2.getDepartment().getInstitution().getName();
                int institutionCompare = institution1.compareTo(institution2);
                if (institutionCompare != 0) {
                    return institutionCompare;
                }

                // Compare by Site Name
                String site1 = b1.getDepartment().getSite().getName();
                String site2 = b2.getDepartment().getSite().getName();
                int siteCompare = site1.compareTo(site2);
                if (siteCompare != 0) {
                    return siteCompare;
                }

                // Compare by Department Name
                String department1 = b1.getDepartment().getName();
                String department2 = b2.getDepartment().getName();
                int departmentCompare = department1.compareTo(department2);
                if (departmentCompare != 0) {
                    return departmentCompare;
                }

                // Compare by Type (If applicable)
                // Assuming there is a 'type' field to be compared as a String or Enum
                String type1 = b1.getBundleType(); // Adjust this depending on how type is defined
                String type2 = b2.getBundleType(); // Adjust this depending on how type is defined
                return type1.compareTo(type2);
            }
        });
    }

    public void setBundles(List<ReportTemplateRowBundle> bundles) {
        this.bundles = bundles;
    }

    public List<ReportTemplateRowBundle> getSelectedBundles() {
        List<ReportTemplateRowBundle> selectedBundles = new ArrayList<>();
        for (ReportTemplateRowBundle b : getBundles()) {
            if (b.isSelected()) {
                selectedBundles.add(b);
            }
        }
        return selectedBundles;
    }

    public Double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(Double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public Double getHospitalTotal() {
        return hospitalTotal;
    }

    public void setHospitalTotal(Double hospitalTotal) {
        this.hospitalTotal = hospitalTotal;
    }

    public Double getStaffTotal() {
        return staffTotal;
    }

    public void setStaffTotal(Double staffTotal) {
        this.staffTotal = staffTotal;
    }

    public Double getCcTotal() {
        return ccTotal;
    }

    public void setCcTotal(Double ccTotal) {
        this.ccTotal = ccTotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Bill getStartBill() {
        return startBill;
    }

    public void setStartBill(Bill startBill) {
        this.startBill = startBill;
    }

    public Bill getEndBill() {
        return endBill;
    }

    public void setEndBill(Bill endBill) {
        this.endBill = endBill;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public double getOnCallHandoverValue() {
        return onCallHandoverValue;
    }

    public void setOnCallHandoverValue(double onCallHandoverValue) {
        this.onCallHandoverValue = onCallHandoverValue;
    }

    public double getCashHandoverValue() {
        return cashHandoverValue;
    }

    public void setCashHandoverValue(double cashHandoverValue) {
        this.cashHandoverValue = cashHandoverValue;
    }

    public double getCardHandoverValue() {
        return cardHandoverValue;
    }

    public void setCardHandoverValue(double cardHandoverValue) {
        this.cardHandoverValue = cardHandoverValue;
    }

    public double getMultiplePaymentMethodsHandoverValue() {
        return multiplePaymentMethodsHandoverValue;
    }

    public void setMultiplePaymentMethodsHandoverValue(double multiplePaymentMethodsHandoverValue) {
        this.multiplePaymentMethodsHandoverValue = multiplePaymentMethodsHandoverValue;
    }

    public double getStaffHandoverValue() {
        return staffHandoverValue;
    }

    public void setStaffHandoverValue(double staffHandoverValue) {
        this.staffHandoverValue = staffHandoverValue;
    }

    public double getCreditHandoverValue() {
        return creditHandoverValue;
    }

    public void setCreditHandoverValue(double creditHandoverValue) {
        this.creditHandoverValue = creditHandoverValue;
    }

    public double getStaffWelfareHandoverValue() {
        return staffWelfareHandoverValue;
    }

    public void setStaffWelfareHandoverValue(double staffWelfareHandoverValue) {
        this.staffWelfareHandoverValue = staffWelfareHandoverValue;
    }

    public double getVoucherHandoverValue() {
        return voucherHandoverValue;
    }

    public void setVoucherHandoverValue(double voucherHandoverValue) {
        this.voucherHandoverValue = voucherHandoverValue;
    }

    public double getIouHandoverValue() {
        return iouHandoverValue;
    }

    public void setIouHandoverValue(double iouHandoverValue) {
        this.iouHandoverValue = iouHandoverValue;
    }

    public double getAgentHandoverValue() {
        return agentHandoverValue;
    }

    public void setAgentHandoverValue(double agentHandoverValue) {
        this.agentHandoverValue = agentHandoverValue;
    }

    public double getChequeHandoverValue() {
        return chequeHandoverValue;
    }

    public void setChequeHandoverValue(double chequeHandoverValue) {
        this.chequeHandoverValue = chequeHandoverValue;
    }

    public double getSlipHandoverValue() {
        return slipHandoverValue;
    }

    public void setSlipHandoverValue(double slipHandoverValue) {
        this.slipHandoverValue = slipHandoverValue;
    }

    public double getEwalletHandoverValue() {
        return eWalletHandoverValue;
    }

    public void setEwalletHandoverValue(double eWalletHandoverValue) {
        this.eWalletHandoverValue = eWalletHandoverValue;
    }

    public double getPatientDepositHandoverValue() {
        return patientDepositHandoverValue;
    }

    public void setPatientDepositHandoverValue(double patientDepositHandoverValue) {
        this.patientDepositHandoverValue = patientDepositHandoverValue;
    }

    public double getPatientPointsHandoverValue() {
        return patientPointsHandoverValue;
    }

    public void setPatientPointsHandoverValue(double patientPointsHandoverValue) {
        this.patientPointsHandoverValue = patientPointsHandoverValue;
    }

    public double getOnlineSettlementHandoverValue() {
        return onlineSettlementHandoverValue;
    }

    public void setOnlineSettlementHandoverValue(double onlineSettlementHandoverValue) {
        this.onlineSettlementHandoverValue = onlineSettlementHandoverValue;
    }

    //    public SessionController getSessionController() {
//        return sessionController;
//    }
//
//    public void setSessionController(SessionController sessionController) {
//        this.sessionController = sessionController;
//    }
    public List<DenominationTransaction> getDenominationTransactions() {
        return denominationTransactions;
    }

    public void setDenominationTransactions(List<DenominationTransaction> denominationTransactions) {
        this.denominationTransactions = denominationTransactions;
    }

    public void calculateTotalHandoverByDenominationQuantities() {
        denominatorValue = 0.0;
        if (denominationTransactions == null || denominationTransactions.isEmpty()) {
            return;
        }
        for (DenominationTransaction dt : denominationTransactions) {
            if (dt == null || dt.getDenomination() == null || dt.getDenomination().getDenominationValue() == null) {
                continue;
            }
            if (dt.getDenominationQty() == null) {
                dt.setDenominationQty(0l);
                dt.setDenominationValue(null);
            } else {
                Double dv = dt.getDenomination().getDenominationValue() * dt.getDenominationQty();
                dt.setDenominationValue(dv);
                denominatorValue += dv;
            }
        }
    }

    private List<DenominationTransaction> createDefaultDenominationTransaction(PaymentMethod pm) {
        List<DenominationTransaction> dts = new ArrayList<>();
        if (denominations == null) {
            return dts;
        }
//        List<com.divudi.entity.cashTransaction.Denomination> denominations = sessionController.findDefaultDenominations();
        for (com.divudi.entity.cashTransaction.Denomination d : denominations) {
            DenominationTransaction dt = new DenominationTransaction();
            dt.setDenomination(d);
            dt.setPaymentMethod(pm);
            dts.add(dt);
        }
        return dts;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public WebUser getFromUser() {
        return fromUser;
    }

    public void setFromUser(WebUser fromUser) {
        this.fromUser = fromUser;
    }

    public WebUser getToUser() {
        return toUser;
    }

    public void setToUser(WebUser toUser) {
        this.toUser = toUser;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public double getDenominatorValue() {
        return denominatorValue;
    }

    public void setDenominatorValue(double denominatorValue) {
        this.denominatorValue = denominatorValue;
    }

    public PaymentHandover getPaymentHandover() {
        return paymentHandover;
    }

    public void setPaymentHandover(PaymentHandover paymentHandover) {
        this.paymentHandover = paymentHandover;
    }

    public List<com.divudi.entity.cashTransaction.Denomination> getDenominations() {
        return denominations;
    }

    public void setDenominations(List<com.divudi.entity.cashTransaction.Denomination> denominations) {
        this.denominations = denominations;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Boolean getSelectAllCashToHandover() {
        if (selectAllCashToHandover == null) {
            selectAllCashToHandover = true;
        }
        return selectAllCashToHandover;
    }

    public void setSelectAllCashToHandover(Boolean selectAllCashToHandover) {
        this.selectAllCashToHandover = selectAllCashToHandover;
    }

}
