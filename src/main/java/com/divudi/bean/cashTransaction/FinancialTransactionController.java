package com.divudi.bean.cashTransaction;
// <editor-fold defaultstate="collapsed" desc="Imports">

import java.util.HashMap;
// </editor-fold>  
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.entity.Bill;
import com.divudi.entity.Payment;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.AtomicBillTypeTotals;
import com.divudi.data.BillFinanceType;
import static com.divudi.data.BillType.CollectingCentreBill;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.FinancialReport;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PaymentMethodValues;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import org.bouncycastle.mail.smime.handlers.pkcs7_mime;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class FinancialTransactionController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillFacade billFacade;
    @EJB
    PaymentFacade paymentFacade;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    BillController billController;
    @Inject
    PaymentController paymentController;
    @Inject
    SearchController searchController;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private Bill currentBill;
    private Payment currentPayment;
    private Payment removingPayment;
    private List<Payment> currentBillPayments;
    private List<Bill> currentBills;
    private List<Bill> fundTransferBillsToReceive;
    private List<Bill> fundBillsForClosureBills;
    private Bill selectedBill;
    private Bill nonClosedShiftStartFundBill;
    private List<Payment> paymentsFromShiftSratToNow;
    private List<Payment> recievedBIllPayments;
    private List<Bill> allBillsShiftStartToNow;
    @Deprecated
    private PaymentMethodValues paymentMethodValues;
    private AtomicBillTypeTotals atomicBillTypeTotalsByBills;
    private AtomicBillTypeTotals atomicBillTypeTotalsByPayments;
    private FinancialReport financialReportByBills;
    private FinancialReport financialReportByPayments;
    //Billed Totals
    private double totalOpdBillValue;
    private double totalPharmecyBillValue;
    private double totalChannelBillValue;
    private double totalCcBillValue;
    private double totalProfessionalPaymentBillValue;

    //Cancelled Totals
    private double totalOpdBillCanceledValue;
    private double totalPharmecyBillCanceledValue;
    private double totalChannelBillCancelledValue;
    private double totalCcBillCanceledValue;
    private double totalProfessionalPaymentBillCancelledValue;

    //Refund Totals
    private double totalOpdBillRefundValue;
    private double totalPharmacyBillRefundValue;
    private double totalChannelBillRefundValue;
    private double totalCcBillRefundValue;

    //Totals
    private double totalBillRefundValue;
    private double totalBillCancelledValue;
    private double totalBilledBillValue;

    private double totalShiftStartValue;
    private double totalBalanceTransfer;
    private double totalTransferRecive;

    private double totalFunds;
    private double shiftEndTotalValue;
    private double shiftEndRefundBillValue;
    private double shiftEndCanceledBillValue;
    private double totalWithdrawals;
    private double totalDeposits;

    private double Deductions;
    private double additions;

    private int fundTransferBillsToReceiveCount;

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public FinancialTransactionController() {
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Navigational Methods">
    public String navigateToFinancialTransactionIndex() {
        resetClassVariables();
        fillFundTransferBillsForMeToReceive();
        return "/cashier/index?faces-redirect=true;";
    }

    public String navigateToCreateNewInitialFundBill() {
        resetClassVariables();
        prepareToAddNewInitialFundBill();
        return "/cashier/initial_fund_bill?faces-redirect=true;";
    }

    public String navigateToFundTransferBill() {
        resetClassVariables();
        prepareToAddNewFundTransferBill();
        return "/cashier/fund_transfer_bill?faces-redirect=true";
    }

    public String navigateToFundDepositBill() {
        resetClassVariables();
        prepareToAddNewFundDepositBill();
        return "/cashier/deposit_funds?faces-redirect=true";
    }

    public String navigateToCashierSummary() {
        return "/cashier/cashier_summary?faces-redirect=true";
    }

    public String navigateToCashierSummaryBreakdown() {
        return "/cashier/shift_end_summary_breakdown?faces-redirect=true";
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForCash() {
        List<PaymentMethod> pms = new ArrayList<>();
        pms.add(PaymentMethod.Cash);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms);
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForCard() {
        List<PaymentMethod> pms = new ArrayList<>();
        pms.add(PaymentMethod.Card);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms);
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForVoucher() {
        List<PaymentMethod> pms = new ArrayList<>();
        pms.add(PaymentMethod.Voucher);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms);
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForAllExceptCashCardVaucher() {
        List<PaymentMethod> pms = PaymentMethod.asList();
        pms.remove(PaymentMethod.Voucher);
        pms.remove(PaymentMethod.Cash);
        pms.remove(PaymentMethod.Card);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms);
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosing(List<PaymentMethod> pms) {
        searchController.setWebUser(sessionController.getLoggedUser());
        searchController.setStartBillId(nonClosedShiftStartFundBill.getId());
        searchController.setEndBillId(null);
        searchController.setPaymentMethods(pms);
        searchController.processAllFinancialTransactionalSummarybySingleUserByIds();
        return "/cashier/shift_end_summary_breakdown?faces-redirect=true";
    }

    public String navigateToReceiveNewFundTransferBill() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        if (selectedBill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
        resetClassVariablesWithoutSelectedBill();
        prepareToAddNewFundTransferReceiveBill();
        return "/cashier/fund_transfer_receive_bill?faces-redirect=true";
    }

    public String navigateToReceiveFundTransferBillsForMe() {
        fillFundTransferBillsForMeToReceive();
        return "/cashier/fund_transfer_bills_for_me_to_receive?faces-redirect=true";
    }

    private void prepareToAddNewInitialFundBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.ShiftStartFundBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_START_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewFundTransferBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.FundTransferBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_TRANSFER_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewFundDepositBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.DepositFundBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_DEPOSIT_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewFundTransferReceiveBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.FundTransferReceivedBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL);

        currentBill.setBillClassType(BillClassType.Bill);
        currentBill.setReferenceBill(selectedBill);
        if (selectedBill != null) {
        }
        currentBillPayments = new ArrayList<>();
        if (selectedBill.getPayments() == null || selectedBill.getPayments().isEmpty()) {
            selectedBill.setPayments(findPaymentsForBill(selectedBill));
        }

        for (Payment p : selectedBill.getPayments()) {
            Payment np = p.copyAttributes();
            currentBillPayments.add(np);

        }
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public void resetClassVariables() {
        currentBill = null;
        currentPayment = null;
        removingPayment = null;
        currentBillPayments = null;
        fundTransferBillsToReceive = null;
        fundBillsForClosureBills = null;
        selectedBill = null;
        nonClosedShiftStartFundBill = null;
        paymentsFromShiftSratToNow = null;
    }

    public void resetClassVariablesWithoutSelectedBill() {
        currentBill = null;
        currentPayment = null;
        removingPayment = null;
        //currentBillPayments = null;
        fundTransferBillsToReceive = null;
        fundBillsForClosureBills = null;
        nonClosedShiftStartFundBill = null;
        paymentsFromShiftSratToNow = null;
    }

    public List<Payment> findPaymentsForBill(Bill b) {
        String jpql = "select p "
                + " from Payment p "
                + " where p.retired=:ret "
                + " and p.bill=:b"
                + " order by p.id";
        Map m = new HashMap();
        m.put("b", b);
        m.put("ret", false);
        return paymentFacade.findByJpql(jpql, m);
    }

    public void addPaymentToInitialFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.ShiftStartFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateInitialFundBillTotal();
        currentPayment = null;
    }

    public void addPaymentToFundTransferBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateFundTransferBillTotal();
        currentPayment = null;
    }

    public void addPaymentToShiftEndFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.ShiftEndFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateShiftEndFundBillTotal();
        currentPayment = null;
    }

    public void addPaymentToWithdrawalFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.WithdrawalFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateWithdrawalFundBillTotal();
        currentPayment = null;
    }

    public void removePayment() {
        getCurrentBillPayments().remove(removingPayment);
        calculateInitialFundBillTotal();
        currentPayment = null;
    }

    private void calculateInitialFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateFundTransferBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateShiftEndFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateWithdrawalFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    public String settleInitialFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.ShiftStartFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        findNonClosedShiftStartFundBillIsAvailable();
        if (nonClosedShiftStartFundBill != null) {
            JsfUtil.addErrorMessage("A shift start fund bill is already available for closure.");
            return "";
        }

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/initial_fund_bill_print?faces-redirect=true";
    }

    public String settleFundTransferBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getToStaff() == null) {
            JsfUtil.addErrorMessage("Select to whom to transfer");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        currentBill.getPayments().addAll(currentBillPayments);
        billController.save(currentBill);
        return "/cashier/fund_transfer_bill_print?faces-redirect=true";
    }

    public String settleWithdrawalFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.WithdrawalFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/fund_withdrawal_bill_print?faces-redirect=true";
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Sample Code Block">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="ShiftEndFundBill">
    public String navigateToCreateShiftEndSummaryBill() {
        resetClassVariables();
        findNonClosedShiftStartFundBillIsAvailable();
        fillPaymentsFromShiftStartToNow();
        if (nonClosedShiftStartFundBill != null) {
            currentBill = new Bill();
            currentBill.setBillType(BillType.ShiftEndFundBill);
            currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_END_BILL);
            currentBill.setBillClassType(BillClassType.Bill);
            currentBill.setReferenceBill(nonClosedShiftStartFundBill);
        } else {
            currentBill = null;
        }
        return "/cashier/shift_end_summery_bill?faces-redirect=true";
    }

    public String navigateToCreateShiftEndSummaryBillByBills() {
        resetClassVariables();
        findNonClosedShiftStartFundBillIsAvailable();
        fillBillsFromShiftStartToNow();
        fillPaymentsFromShiftStartToNow();
        if (nonClosedShiftStartFundBill != null) {
            currentBill = new Bill();
            currentBill.setBillType(BillType.ShiftEndFundBill);
            currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_END_BILL);
            currentBill.setBillClassType(BillClassType.Bill);
            currentBill.setReferenceBill(nonClosedShiftStartFundBill);
        } else {
            currentBill = null;
        }
        return "/cashier/shift_end_summery_bill_by_bills?faces-redirect=true";
    }

    public void fillPaymentsFromShiftStartToNow() {
        paymentsFromShiftSratToNow = new ArrayList<>();
        if (nonClosedShiftStartFundBill == null) {
            return;
        }
        Long shiftStartBillId = nonClosedShiftStartFundBill.getId();
        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.creater = :cr "
                + "AND p.retired = :ret "
                + "AND p.id > :cid "
                + "ORDER BY p.id DESC";
        Map<String, Object> m = new HashMap<>();
        m.put("cr", nonClosedShiftStartFundBill.getCreater());
        m.put("ret", false);
        m.put("cid", nonClosedShiftStartFundBill.getId());
        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m);
//        paymentMethodValues = new PaymentMethodValues(PaymentMethod.values());
        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();
        for (Payment p : paymentsFromShiftSratToNow) {
            if (p.getBill().getBillTypeAtomic() == null) {
            } else {
                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(p.getBill().getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
            }
            //            calculateBillValuesFromBillTypes(p);
        }
//        calculateTotalFundsFromShiftStartToNow();
        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);
        
    }

    public void fillBillsFromShiftStartToNow() {
        currentBills = new ArrayList<>();
        if (nonClosedShiftStartFundBill == null) {
            return;
        }
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_DECREASE));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_INCREASE));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_STARTING_BALANCE));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_CLOSING_BALANCE));

        Long shiftStartBillId = nonClosedShiftStartFundBill.getId();
        String jpql = "SELECT p "
                + "FROM Bill p "
                + "WHERE p.creater = :cr "
                + "AND p.retired = :ret "
                + "AND p.billTypeAtomic in :btas "
                + "AND p.id > :cid "
                + "ORDER BY p.id DESC";
        Map<String, Object> m = new HashMap<>();
        m.put("cr", nonClosedShiftStartFundBill.getCreater());
        m.put("btas", billTypesToFilter);
        m.put("ret", false);
        m.put("cid", shiftStartBillId);
        currentBills = billFacade.findByJpql(jpql, m);
//        paymentMethodValues = new PaymentMethodValues(PaymentMethod.values());
        atomicBillTypeTotalsByBills = new AtomicBillTypeTotals();
        for (Bill p : currentBills) {
            if (p.getBillTypeAtomic() == null) {
            } else {
            }
            atomicBillTypeTotalsByBills.addOrUpdateAtomicRecord(p.getBillTypeAtomic(), p.getPaymentMethod(), p.getNetTotal());
//            calculateBillValuesFromBillTypes(p);
        }
        financialReportByBills = new FinancialReport(atomicBillTypeTotalsByBills);
        nonClosedShiftStartFundBill.setTotal(financialReportByBills.getTotal());
        nonClosedShiftStartFundBill.setNetTotal(financialReportByBills.getTotal());
    }

//    public void calculateBillValuesFromBillTypes(Payment p) {
//        if (p.getBill() == null) {
//            return;
//        }
//        if (p.getBill().getBillType() == null) {
//            return;
//        }
//        if (p.getBill().getBillTypeAtomic() == null) {
//            return;
//        }
//
//        switch (p.getBill().getBillTypeAtomic().getBillCategory()) {
//            case BILL:
//                if (p.getPaidValue() != 0.0) {
//                    paymentMethodValues.addValue(p);
//                } else {
//                    paymentMethodValues.addValue(p.getBill());
//                }
//                break;
//            case CANCELLATION:
//            case REFUND:
//                if (p.getPaidValue() != 0.0) {
//                    paymentMethodValues.deductAbsoluteValue(p);
//                } else {
//                    paymentMethodValues.deductAbsoluteValue(p.getBill());
//                }
//                break;
//            default:
//                break;
//
//        }
//    }
    public void calculateBillValuesFromBillTypes(Bill p) {
        if (p == null) {
            return;
        }
        if (p.getBillType() == null) {
            return;
        }
        if (p.getBillTypeAtomic() == null) {
            return;
        }

        paymentMethodValues.addValue(p);

//        switch (p.getBillTypeAtomic().getBillCategory()) {
//            case BILL:
//                if (p.getNetTotal() != 0.0) {
//                    paymentMethodValues.addValue(p);
//                } else {
//                    paymentMethodValues.addValue(p);
//                }
//                break;
//            case CANCELLATION:
//            case REFUND:
//                if (p.getNetTotal() != 0.0) {
//                    paymentMethodValues.deductAbsoluteValue(p);
//                } else {
//                    paymentMethodValues.deductAbsoluteValue(p);
//                }
//                break;
//            default:
//                break;
//
//        }
    }

    public void calculateTotalFundsFromShiftStartToNow() {
        additions = financialReportByPayments.getCashTotal()+financialReportByPayments.getNetCreditCardTotal()+financialReportByPayments.getCollectedVoucher()+financialReportByPayments.getNetOtherNonCreditTotal()+financialReportByPayments.getBankWithdrawals();
        Deductions = financialReportByPayments.getRefundedCash()+financialReportByPayments.getRefundedCreditCard()+financialReportByPayments.getRefundedVoucher()+financialReportByPayments.getRefundedOtherNonCredit()+financialReportByPayments.getFloatHandover()+financialReportByPayments.getBankDeposits();
        totalFunds = additions - Deductions;
        shiftEndTotalValue = totalFunds;

    }

    public void resetTotalFundsValues() {
        totalOpdBillValue = 0.0;
        totalPharmecyBillValue = 0.0;
        totalChannelBillValue = 0.0;
        totalCcBillValue = 0.0;
        totalProfessionalPaymentBillValue = 0.0;

        totalOpdBillCanceledValue = 0.0;
        totalPharmecyBillCanceledValue = 0.0;
        totalChannelBillCancelledValue = 0.0;
        totalCcBillCanceledValue = 0.0;
        totalProfessionalPaymentBillCancelledValue = 0.0;

        totalOpdBillRefundValue = 0.0;
        totalPharmacyBillRefundValue = 0.0;
        totalChannelBillRefundValue = 0.0;
        totalCcBillRefundValue = 0.0;

        totalBillRefundValue = 0.0;
        totalBillCancelledValue = 0.0;
        totalBilledBillValue = 0.0;

        totalShiftStartValue = 0.0;
        totalBalanceTransfer = 0.0;
        totalTransferRecive = 0.0;

        totalFunds = 0.0;
        shiftEndTotalValue = 0.0;
        shiftEndRefundBillValue = 0.0;
        shiftEndCanceledBillValue = 0.0;
        totalWithdrawals = 0.0;
        totalDeposits = 0.0;

        Deductions = 0.0;
        additions = 0.0;

        fundTransferBillsToReceiveCount = 0;
    }

    public void findNonClosedShiftStartFundBillIsAvailable() {
        nonClosedShiftStartFundBill = null;
        String jpql = "select b "
                + " from Bill b "
                + " where b.staff=:staff "
                + " and b.retired=:ret "
                + " and b.billType=:ofb "
                + " and b.referenceBill is null";
        Map m = new HashMap();
        m.put("staff", sessionController.getLoggedUser().getStaff());
        m.put("ret", false);
        m.put("ofb", BillType.ShiftStartFundBill);
        nonClosedShiftStartFundBill = billFacade.findFirstByJpql(jpql, m);
    }

    public void listBillsFromInitialFundBillUpToNow() {
        List<Bill> shiftStartFundBill;
        String jpql = "select b "
                + " from Bill b "
                + " where b.staff=:staff "
                + " and b.retired=:ret "
                + " and b.billType=:ofb "
                + " and b.referenceBill is null";
        Map m = new HashMap();
        m.put("staff", sessionController.getLoggedUser().getStaff());
        m.put("ret", false);
        m.put("ofb", BillType.ShiftStartFundBill);
        shiftStartFundBill = billFacade.findByJpql(jpql, m);

    }

    public String settleShiftEndFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.ShiftEndFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        billController.save(currentBill);
        currentBill.setTotal(financialReportByPayments.getTotal());
        currentBill.setNetTotal(financialReportByPayments.getTotal());
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        calculateTotalFundsFromShiftStartToNow();
        nonClosedShiftStartFundBill.setReferenceBill(currentBill);
        billController.save(nonClosedShiftStartFundBill);
        
        return "/cashier/shift_end_summery_bill_print?faces-redirect=true";
    }

// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="BalanceTransferFundBill">
    /**
     *
     * User click to Crete Transfer Bill Add (fromStaff 0 the user) Select User
     * to transfer (toStaff) settle to print
     *
     */
// </editor-fold>   
// <editor-fold defaultstate="collapsed" desc="BalanceTransferReceiveFundBill">
    /**
     *
     * pavan
     *
     * Another User create a BalanceTransferFundBill It has a toStaff attribute
     * loggedUser.getStaff =toStaff List such bills Click on one of them Copy
     * Payments from BalanceTransferFundBill User may change them settle to
     * print
     *
     * @return
     */
    public void fillFundTransferBillsForMeToReceive() {
        String sql;
        fundTransferBillsToReceive = null;
        Map tempMap = new HashMap();
        sql = "select s "
                + "from Bill s "
                + "where s.retired=:ret "
                + "and s.billType=:btype "
                + "and s.toStaff=:logStaff "
                + "and s.referenceBill is null "
                + "order by s.createdAt ";
        tempMap.put("btype", BillType.FundTransferBill);
        tempMap.put("ret", false);
        tempMap.put("logStaff", sessionController.getLoggedUser().getStaff());
        fundTransferBillsToReceive = billFacade.findByJpql(sql, tempMap);
        fundTransferBillsToReceiveCount = fundTransferBillsToReceive.size();

    }

    public String settleFundTransferReceiveBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }

        if (currentBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }

        if (currentBill.getBillType() != BillType.FundTransferReceivedBill) {
            JsfUtil.addErrorMessage("Error - bill type");
            return "";
        }

        if (currentBill.getReferenceBill().getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Error - Reference bill type");
            return "";
        }

        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setToStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(currentBill.getReferenceBill().getFromStaff());
        billController.save(currentBill);
        for (Payment p : currentBillPayments) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        currentBill.getReferenceBill().setReferenceBill(currentBill);
        billController.save(currentBill.getReferenceBill());

        return "/cashier/fund_transfer_receive_bill_print?faces-redirect=true";
    }

// </editor-fold>      
// <editor-fold defaultstate="collapsed" desc="DepositFundBill">
    //Lawan
    public void addPaymentToFundDepositBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.DepositFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateFundDepositBillTotal();
        currentPayment = null;
    }

    private void calculateFundDepositBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    public String settleFundDepositBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.DepositFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/deposit_funds_print?faces-redirect=true";
    }
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="WithdrawalFundBill">

    public String navigateToCreateNewFundWithdrawalBill() {
        prepareToAddNewWithdrawalProcessingBill();
        return "/cashier/fund_withdrawal_bill?faces-redirect=true;";
    }

    private void prepareToAddNewWithdrawalProcessingBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.WithdrawalFundBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_WITHDRAWAL_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

//Damith
// </editor-fold>      
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Bill getCurrentBill() {
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public Payment getCurrentPayment() {
        if (currentPayment == null) {
            currentPayment = new Payment();
        }
        return currentPayment;
    }

    public void setCurrentPayment(Payment currentPayment) {
        this.currentPayment = currentPayment;
    }

    public List<Payment> getCurrentBillPayments() {
        if (currentBillPayments == null) {
            currentBillPayments = new ArrayList<>();
        }
        return currentBillPayments;
    }

    public void setCurrentBillPayments(List<Payment> currentBillPayments) {
        this.currentBillPayments = currentBillPayments;
    }

    public Payment getRemovingPayment() {
        return removingPayment;
    }

    public void setRemovingPayment(Payment removingPayment) {
        this.removingPayment = removingPayment;
    }

    public List<Bill> getFundTransferBillsToReceive() {
        return fundTransferBillsToReceive;
    }

    public void setFundTransferBillsToReceive(List<Bill> fundTransferBillsToReceive) {
        this.fundTransferBillsToReceive = fundTransferBillsToReceive;
    }

    public Bill getSelectedBill() {
        return selectedBill;
    }

    public void setSelectedBill(Bill selectedBill) {
        this.selectedBill = selectedBill;
    }

    // </editor-fold>  
    public List<Bill> getFundBillsForClosureBills() {
        return fundBillsForClosureBills;
    }

    public void setFundBillsForClosureBills(List<Bill> fundBillsForClosureBills) {
        this.fundBillsForClosureBills = fundBillsForClosureBills;
    }

    public Bill getNonClosedShiftStartFundBill() {
        return nonClosedShiftStartFundBill;
    }

    public void setNonClosedShiftStartFundBill(Bill nonClosedShiftStartFundBill) {
        this.nonClosedShiftStartFundBill = nonClosedShiftStartFundBill;
    }

    public List<Payment> getPaymentsFromShiftSratToNow() {
        if (paymentsFromShiftSratToNow == null) {
            paymentsFromShiftSratToNow = new ArrayList<>();
        }
        return paymentsFromShiftSratToNow;
    }

    public void setPaymentsFromShiftSratToNow(List<Payment> paymentsFromShiftSratToNow) {
        this.paymentsFromShiftSratToNow = paymentsFromShiftSratToNow;
    }

    public List<Bill> getAllBillsShiftStartToNow() {
        if (allBillsShiftStartToNow == null) {
            allBillsShiftStartToNow = new ArrayList<>();
        }
        return allBillsShiftStartToNow;
    }

    public void setAllBillsShiftStartToNow(List<Bill> allBillsShiftStartToNow) {
        this.allBillsShiftStartToNow = allBillsShiftStartToNow;
    }

    public double getTotalOpdBillValue() {
        return totalOpdBillValue;
    }

    public void setTotalOpdBillValue(double totalOpdBillValue) {
        this.totalOpdBillValue = totalOpdBillValue;
    }

    public double getTotalPharmecyBillValue() {
        return totalPharmecyBillValue;
    }

    public void setTotalPharmecyBillValue(double totalPharmecyBillValue) {
        this.totalPharmecyBillValue = totalPharmecyBillValue;
    }

    public double getTotalShiftStartValue() {
        return totalShiftStartValue;
    }

    public void setTotalShiftStartValue(double totalShiftStartValue) {
        this.totalShiftStartValue = totalShiftStartValue;
    }

    public double getTotalBalanceTransfer() {
        return totalBalanceTransfer;
    }

    public void setTotalBalanceTransfer(double totalBalanceTransfer) {
        this.totalBalanceTransfer = totalBalanceTransfer;
    }

    public double getTotalTransferRecive() {
        return totalTransferRecive;
    }

    public void setTotalTransferRecive(double totalTransferRecive) {
        this.totalTransferRecive = totalTransferRecive;
    }

    public double getTotalFunds() {
        return totalFunds;
    }

    public void setTotalFunds(double totalFunds) {
        this.totalFunds = totalFunds;
    }

    public double getShiftEndTotalValue() {
        return shiftEndTotalValue;
    }

    public void setShiftEndTotalValue(double shiftEndTotalValue) {
        this.shiftEndTotalValue = shiftEndTotalValue;
    }

    public double getShiftEndRefundBillValue() {
        return shiftEndRefundBillValue;
    }

    public void setShiftEndRefundBillValue(double shiftEndRefundBillValue) {
        this.shiftEndRefundBillValue = shiftEndRefundBillValue;
    }

    public double getShiftEndCanceledBillValue() {
        return shiftEndCanceledBillValue;
    }

    public void setShiftEndCanceledBillValue(double shiftEndCanceledBillValue) {
        this.shiftEndCanceledBillValue = shiftEndCanceledBillValue;
    }

    public double getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(double totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public double getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(double totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public double getTotalBillRefundValue() {
        return totalBillRefundValue;
    }

    public void setTotalBillRefundValue(double totalBillRefundValue) {
        this.totalBillRefundValue = totalBillRefundValue;
    }

    public double getTotalBillCancelledValue() {
        return totalBillCancelledValue;
    }

    public void setTotalBillCancelledValue(double totalBillCancelledValue) {
        this.totalBillCancelledValue = totalBillCancelledValue;
    }

    public double getTotalBilledBillValue() {
        return totalBilledBillValue;
    }

    public void setTotalBilledBillValue(double totalBilledBillValue) {
        this.totalBilledBillValue = totalBilledBillValue;
    }

    public double getDeductions() {
        return Deductions;
    }

    public void setDeductions(double Deductions) {
        this.Deductions = Deductions;
    }

    public double getAdditions() {
        return additions;
    }

    public void setAdditions(double additions) {
        this.additions = additions;
    }

    public double getTotalCcBillValue() {
        return totalCcBillValue;
    }

    public void setTotalCcBillValue(double totalCcBillValue) {
        this.totalCcBillValue = totalCcBillValue;
    }

    public double getTotalOpdBillCanceledValue() {
        return totalOpdBillCanceledValue;
    }

    public void setTotalOpdBillCanceledValue(double totalOpdBillCanceledValue) {
        this.totalOpdBillCanceledValue = totalOpdBillCanceledValue;
    }

    public double getTotalPharmecyBillCanceledValue() {
        return totalPharmecyBillCanceledValue;
    }

    public void setTotalPharmecyBillCanceledValue(double totalPharmecyBillCanceledValue) {
        this.totalPharmecyBillCanceledValue = totalPharmecyBillCanceledValue;
    }

    public double getTotalCcBillCanceledValue() {
        return totalCcBillCanceledValue;
    }

    public void setTotalCcBillCanceledValue(double totalCcBillCanceledValue) {
        this.totalCcBillCanceledValue = totalCcBillCanceledValue;
    }

    public int getFundTransferBillsToReceiveCount() {
        return fundTransferBillsToReceiveCount;
    }

    public void setFundTransferBillsToReceiveCount(int fundTransferBillsToReceiveCount) {
        this.fundTransferBillsToReceiveCount = fundTransferBillsToReceiveCount;
    }

    public List<Payment> getRecievedBIllPayments() {
        return recievedBIllPayments;
    }

    public void setRecievedBIllPayments(List<Payment> recievedBIllPayments) {
        this.recievedBIllPayments = recievedBIllPayments;
    }

    public double getTotalChannelBillValue() {
        return totalChannelBillValue;
    }

    public void setTotalChannelBillValue(double totalChannelBillValue) {
        this.totalChannelBillValue = totalChannelBillValue;
    }

    public double getTotalChannelBillCancelledValue() {
        return totalChannelBillCancelledValue;
    }

    public void setTotalChannelBillCancelledValue(double totalChannelBillCancelledValue) {
        this.totalChannelBillCancelledValue = totalChannelBillCancelledValue;
    }

    public double getTotalOpdBillRefundValue() {
        return totalOpdBillRefundValue;
    }

    public void setTotalOpdBillRefundValue(double totalOpdBillRefundValue) {
        this.totalOpdBillRefundValue = totalOpdBillRefundValue;
    }

    public double getTotalPharmacyBillRefundValue() {
        return totalPharmacyBillRefundValue;
    }

    public void setTotalPharmacyBillRefundValue(double totalPharmacyBillRefundValue) {
        this.totalPharmacyBillRefundValue = totalPharmacyBillRefundValue;
    }

    public double getTotalChannelBillRefundValue() {
        return totalChannelBillRefundValue;
    }

    public void setTotalChannelBillRefundValue(double totalChannelBillRefundValue) {
        this.totalChannelBillRefundValue = totalChannelBillRefundValue;
    }

    public double getTotalCcBillRefundValue() {
        return totalCcBillRefundValue;
    }

    public void setTotalCcBillRefundValue(double totalCcBillRefundValue) {
        this.totalCcBillRefundValue = totalCcBillRefundValue;
    }

    public double getTotalProfessionalPaymentBillValue() {
        return totalProfessionalPaymentBillValue;
    }

    public void setTotalProfessionalPaymentBillValue(double totalProfessionalPaymentBillValue) {
        this.totalProfessionalPaymentBillValue = totalProfessionalPaymentBillValue;
    }

    public double getTotalProfessionalPaymentBillCancelledValue() {
        return totalProfessionalPaymentBillCancelledValue;
    }

    public void setTotalProfessionalPaymentBillCancelledValue(double totalProfessionalPaymentBillCancelledValue) {
        this.totalProfessionalPaymentBillCancelledValue = totalProfessionalPaymentBillCancelledValue;
    }

    public PaymentMethodValues getPaymentMethodValues() {
        return paymentMethodValues;
    }

    public AtomicBillTypeTotals getAtomicBillTypeTotalsByBills() {
        return atomicBillTypeTotalsByBills;
    }

    public void setAtomicBillTypeTotalsByBills(AtomicBillTypeTotals atomicBillTypeTotalsByBills) {
        this.atomicBillTypeTotalsByBills = atomicBillTypeTotalsByBills;
    }

    public FinancialReport getFinancialReportByBills() {
        return financialReportByBills;
    }

    public void setFinancialReportByBills(FinancialReport financialReportByBills) {
        this.financialReportByBills = financialReportByBills;
    }

    public List<Bill> getCurrentBills() {
        return currentBills;
    }

    public void setCurrentBills(List<Bill> currentBills) {
        this.currentBills = currentBills;
    }

    public AtomicBillTypeTotals getAtomicBillTypeTotalsByPayments() {
        return atomicBillTypeTotalsByPayments;
    }

    public void setAtomicBillTypeTotalsByPayments(AtomicBillTypeTotals atomicBillTypeTotalsByPayments) {
        this.atomicBillTypeTotalsByPayments = atomicBillTypeTotalsByPayments;
    }

    public FinancialReport getFinancialReportByPayments() {
        return financialReportByPayments;
    }

    public void setFinancialReportByPayments(FinancialReport financialReportByPayments) {
        this.financialReportByPayments = financialReportByPayments;
    }

}
