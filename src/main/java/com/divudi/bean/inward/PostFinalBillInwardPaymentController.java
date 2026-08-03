/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ControllerWithMultiplePayments;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Records payments received from an inward patient AFTER the final bill has
 * been confirmed - independent from, and invisible to, the pre-final
 * "Inward Payment" (deposit) flow in {@link InwardPaymentController}.
 *
 * See docs/superpowers/specs/2026-08-02-post-final-bill-inward-payment-design.md
 * for the full design (issue #22617).
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PostFinalBillInwardPaymentController implements Serializable, ControllerWithMultiplePayments {

    private static final long serialVersionUID = 1L;

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BilledBillFacade billedBillFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    PaymentService paymentService;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    PatientFacade patientFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private InwardBeanController inwardBean;
    @Inject
    private BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    @Inject
    private FinancialTransactionController financialTransactionController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Variables">
    private BilledBill current;
    /** Dedicated instance for the refund flow (a new RefundBill, distinct from {@link #current}). */
    private RefundBill refundCurrent;
    /** Amount already paid on the post-final payment bill being refunded; set by the caller/page before {@link #refundPostFinalPayment()}. */
    private double paidAmount;
    private boolean printPreview;
    String comment;

    private PaymentMethod paymentMethod;
    private double total;
    private Patient patient;
    private PaymentMethodData paymentMethodData;
    private List<Bill> eligiblePostFinalPaymentBills;
    private Bill originalBillToRefund;
    private Map<Long, Double> remainingRefundableAmountCache;
    // </editor-fold>

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();
    }

    /**
     * Navigate to the post final bill inward payment page, requiring an
     * active shift. If a PatientEncounter has already been set on
     * {@link #getCurrent()} (e.g. by a caller navigating from the Inpatient
     * Dashboard for a specific admission), the confirmed-final-bill entry
     * guard is validated immediately; otherwise (e.g. Main Menu -> Payment
     * Management entry, where the cashier picks the BHT on the page itself)
     * the guard is validated later in {@link #bhtListener()} once a BHT is
     * selected.
     */
    public String navigateToPostFinalPayment() {
        PatientEncounter pe = current != null ? current.getPatientEncounter() : null;
        makeNull();
        financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
        if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
            JsfUtil.addErrorMessage("Start Your Shift First !");
            return "/cashier/index?faces-redirect=true";
        }

        if (pe != null) {
            if (!isFinalBillReadyForPostFinalPayment(pe)) {
                JsfUtil.addErrorMessage("No confirmed final bill found for this admission. Post final bill payments can only be recorded after the final bill is confirmed.");
                return "/inward/admission_profile?faces-redirect=true";
            }
            getCurrent().setPatientEncounter(pe);
            paymentListener();
        }

        return "/inward/inward_bill_post_final_payment?faces-redirect=true";
    }

    private boolean isFinalBillReadyForPostFinalPayment(PatientEncounter pe) {
        if (pe == null) {
            return false;
        }
        Bill finalBill = pe.getFinalBill();
        if (finalBill == null) {
            return false;
        }
        if (finalBill.isCancelled()) {
            return false;
        }
        return finalBill.isConfirmedFinalBill();
    }

    public void bhtListener() {
        if (current.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return;
        }
        if (!isFinalBillReadyForPostFinalPayment(current.getPatientEncounter())) {
            JsfUtil.addErrorMessage("No confirmed final bill found for this admission. Post final bill payments can only be recorded after the final bill is confirmed.");
            current.setPatientEncounter(null);
            return;
        }
        paymentListener();
    }

    public void paymentListener() {
        patient = current.getPatientEncounter().getPatient();
        paymentMethod = null;
        paymentMethodData = new PaymentMethodData();
    }

    /**
     * Live outstanding balance for the current patient encounter's final
     * bill, computed fresh every call (never trusted from a snapshot):
     *
     * balance = finalBill.netTotal - finalBill.paidAmount
     *           - SUM(netTotal of non-cancelled PostFinalBillInwardPayment
     *                 bills for this patientEncounter)
     */
    public double getLiveBalanceDue() {
        if (getCurrent().getPatientEncounter() == null) {
            return 0.0;
        }
        PatientEncounter pe = getCurrent().getPatientEncounter();
        Bill finalBill = pe.getFinalBill();
        if (finalBill == null) {
            return 0.0;
        }
        double alreadyPaidPostFinal = getPostFinalPaymentTotal(pe);
        double balance = finalBill.getNetTotal() - finalBill.getPaidAmount() - alreadyPaidPostFinal;
        return Math.max(0.0, balance);
    }

    /**
     * Deliberately does NOT filter on {@code b.cancelled=false}: cancellation
     * and refund are represented as separate {@code Bill} rows (a
     * {@code CancelledBill}/{@code RefundBill} with a negated {@code netTotal},
     * same {@code billType}) rather than by hiding the original row, so
     * summing every non-retired row of this billType nets out correctly.
     * This mirrors {@link InwardBeanController#getPaidValue(PatientEncounter)},
     * which uses the same unfiltered-sum pattern for the pre-final deposit
     * due calculation - adding a {@code cancelled=false} filter here would
     * double-count a cancellation (excludes the positive original row but
     * still includes the negative cancellation row).
     */
    private double getPostFinalPaymentTotal(PatientEncounter pe) {
        String sql = "Select sum(b.netTotal) From Bill b where"
                + " b.retired=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter=:pe";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.PostFinalBillInwardPayment);
        hm.put("pe", pe);
        return getBillFacade().findDoubleByJpql(sql, hm);
    }

    // <editor-fold defaultstate="collapsed" desc="Print receipt snapshot values">
    // These are deliberately id-scoped ("as of this bill's creation"), not
    // live-scoped like getLiveBalanceDue(): a printed/reprinted receipt must
    // show the same figures every time, even after later payments change the
    // live balance. Bill ids are immutable and monotonically increasing, so
    // "id < this bill's id" is a stable stand-in for "created before this
    // bill" without needing a new persisted snapshot column/migration.

    /** Final bill's net total, for the receipt header. */
    public double getFinalBillNetTotalForBill(Bill bill) {
        Bill finalBill = resolveFinalBill(bill);
        return finalBill != null ? finalBill.getNetTotal() : 0.0;
    }

    /** Everything paid toward the final bill before this payment: the settle-time snapshot plus earlier post-final payments. */
    public double getPaidPreviouslyForBill(Bill bill) {
        if (bill == null || bill.getPatientEncounter() == null) {
            return 0.0;
        }
        Bill finalBill = resolveFinalBill(bill);
        double finalBillPaidAmount = finalBill != null ? finalBill.getPaidAmount() : 0.0;
        return finalBillPaidAmount + getPostFinalPaymentTotalBefore(bill.getPatientEncounter(), bill.getId());
    }

    /** Balance outstanding immediately before this payment was made. */
    public double getDuePreviouslyForBill(Bill bill) {
        return getFinalBillNetTotalForBill(bill) - getPaidPreviouslyForBill(bill);
    }

    /** Balance remaining after this payment (or after this cancellation/refund reverses one). */
    public double getBalanceForBill(Bill bill) {
        if (bill == null) {
            return 0.0;
        }
        return getDuePreviouslyForBill(bill) - bill.getNetTotal();
    }

    private Bill resolveFinalBill(Bill bill) {
        if (bill == null || bill.getPatientEncounter() == null) {
            return null;
        }
        return bill.getPatientEncounter().getFinalBill();
    }

    private double getPostFinalPaymentTotalBefore(PatientEncounter pe, Long billId) {
        String sql = "Select sum(b.netTotal) From Bill b where"
                + " b.retired=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter=:pe "
                + " and b.id < :billId";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.PostFinalBillInwardPayment);
        hm.put("pe", pe);
        hm.put("billId", billId);
        return getBillFacade().findDoubleByJpql(sql, hm);
    }
    // </editor-fold>

    public String navigateToInpationDashbord() {
        return "/inward/admission_profile?faces-redirect=true";
    }

    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return true;
        }

        if (!isFinalBillReadyForPostFinalPayment(getCurrent().getPatientEncounter())) {
            JsfUtil.addErrorMessage("No confirmed final bill found for this admission. Post final bill payments can only be recorded after the final bill is confirmed.");
            return true;
        }

        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please Select a Payment Method");
            return true;
        }

        if (checkErrorsInPaymentMethod(paymentMethod, paymentMethodData)) {
            return true;
        }

        double liveBalanceDue = getLiveBalanceDue();
        if (getCurrent().getTotal() > liveBalanceDue + 0.01) {
            JsfUtil.addErrorMessage("Payment amount exceeds the outstanding balance due (" + liveBalanceDue + "). Please enter a valid amount.");
            return true;
        }

        return false;
    }

    private boolean checkErrorsInPaymentMethod(PaymentMethod method, PaymentMethodData methodData) {
        if (method == null) {
            JsfUtil.addErrorMessage("Please Select a Payment Method");
            return true;
        }
        double amountToCheck = 0.0;

        if (method == PaymentMethod.Cash) {
            amountToCheck = getCurrent().getTotal();
            if (amountToCheck <= 0.0) {
                JsfUtil.addErrorMessage("Please enter a payment amount");
                return true;
            }
        } else {
            if (methodData == null) {
                JsfUtil.addErrorMessage("Error in Payment Data.");
                return true;
            }

            switch (method) {
                case Card:
                    if (methodData.getCreditCard() == null) {
                        JsfUtil.addErrorMessage("Error in card payment Details");
                        return true;
                    } else {
                        if (methodData.getCreditCard().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the card payment amount");
                            return true;
                        }
                        if (methodData.getCreditCard().getInstitution() == null || methodData.getCreditCard().getNo() == null) {
                            JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getCreditCard().getTotalValue();
                    }
                    break;

                case Cheque:
                    if (methodData.getCheque() == null) {
                        JsfUtil.addErrorMessage("Error in Cheque payment Details");
                        return true;
                    } else {
                        if (methodData.getCheque().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the cheque payment amount");
                            return true;
                        }
                        if (methodData.getCheque().getInstitution() == null || methodData.getCheque().getNo() == null || methodData.getCheque().getDate() == null) {
                            JsfUtil.addErrorMessage("Please select Cheque Number, Bank and Cheque Date.");
                            return true;
                        }
                        amountToCheck = methodData.getCheque().getTotalValue();
                    }
                    break;

                case Slip:
                    if (methodData.getSlip() == null) {
                        JsfUtil.addErrorMessage("Error in Slip payment Details");
                        return true;
                    } else {
                        if (methodData.getSlip().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the slip payment details");
                            return true;
                        }
                        if (methodData.getSlip().getInstitution() == null || methodData.getSlip().getDate() == null) {
                            JsfUtil.addErrorMessage("Please Fill Bank and Slip Date.");
                            return true;
                        }
                        amountToCheck = methodData.getSlip().getTotalValue();
                    }
                    break;

                case ewallet:
                    if (methodData.getEwallet() == null) {
                        JsfUtil.addErrorMessage("Error in eWallet payment Details");
                        return true;
                    } else {
                        if (methodData.getEwallet().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the eWallet payment Amount");
                            return true;
                        }
                        if (methodData.getEwallet().getInstitution() == null || ((methodData.getEwallet().getReferenceNo() == null || methodData.getEwallet().getReferenceNo().trim().isEmpty()) && (methodData.getEwallet().getNo() == null || methodData.getEwallet().getNo().trim().isEmpty()))) {
                            JsfUtil.addErrorMessage("Please Fill eWallet Reference Number and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getEwallet().getTotalValue();
                    }
                    break;

                case OnlineSettlement:
                    if (methodData.getOnlineSettlement() == null) {
                        JsfUtil.addErrorMessage("Error in Online Settlement Details");
                        return true;
                    } else {
                        if (methodData.getOnlineSettlement().getTotalValue() <= 0.0) {
                            JsfUtil.addErrorMessage("Please enter the Online Settlement payment Amount");
                            return true;
                        }
                        if (methodData.getOnlineSettlement().getInstitution() == null || methodData.getOnlineSettlement().getReferenceNo() == null || methodData.getOnlineSettlement().getReferenceNo().trim().isEmpty() || methodData.getOnlineSettlement().getDate() == null) {
                            JsfUtil.addErrorMessage("Please Fill Online Settlement Reference Number, Date and Bank.");
                            return true;
                        }
                        amountToCheck = methodData.getOnlineSettlement().getTotalValue();
                    }
                    break;

                case MultiplePaymentMethods:
                    if (methodData.getPaymentMethodMultiple() == null || methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null || methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
                        JsfUtil.addErrorMessage("Please configure payment amounts");
                        return true;
                    }

                    List<ComponentDetail> paymentDetailsList = methodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();

                    if (paymentDetailsList.isEmpty()) {
                        JsfUtil.addErrorMessage("Please select the first payment method.");
                        return true;
                    }

                    if (paymentDetailsList.size() == 1) {
                        JsfUtil.addErrorMessage("You can't use only one payment method.");
                        return true;
                    }

                    double multipleTotal = 0.0;
                    for (ComponentDetail cd : paymentDetailsList) {
                        if (cd != null && cd.getPaymentMethodData() != null) {
                            Double checkAmount = 0.0;
                            switch (cd.getPaymentMethod()) {
                                case Cash:
                                    if (cd.getPaymentMethodData().getCash() == null) {
                                        JsfUtil.addErrorMessage("Please enter the amount to be paid from Cash.");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCash().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter a cash amount");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Card:
                                    if (cd.getPaymentMethodData().getCreditCard() == null) {
                                        JsfUtil.addErrorMessage("Error in card payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCreditCard().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Card.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getCreditCard().getInstitution() == null || cd.getPaymentMethodData().getCreditCard().getNo() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Credit Card Number and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Cheque:
                                    if (cd.getPaymentMethodData().getCheque() == null) {
                                        JsfUtil.addErrorMessage("Error in Cheque payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getCheque().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Cheque.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getCheque().getInstitution() == null || cd.getPaymentMethodData().getCheque().getNo() == null || cd.getPaymentMethodData().getCheque().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please select Cheque Number, Bank and Cheque Date.");
                                            return true;
                                        }

                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case Slip:
                                    if (cd.getPaymentMethodData().getSlip() == null) {
                                        JsfUtil.addErrorMessage("Error in Slip payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getSlip().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Back Slip.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getSlip().getInstitution() == null || cd.getPaymentMethodData().getSlip().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Bank and Slip Date.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                case OnlineSettlement:
                                    if (cd.getPaymentMethodData().getOnlineSettlement() == null) {
                                        JsfUtil.addErrorMessage("Error in Online Settlement Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Online Settlement.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getOnlineSettlement().getInstitution() == null || cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo() == null || cd.getPaymentMethodData().getOnlineSettlement().getReferenceNo().trim().isEmpty() || cd.getPaymentMethodData().getOnlineSettlement().getDate() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Online Settlement Reference Number, Date and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                case Staff_Welfare:
                                    if (cd.getPaymentMethodData().getStaffWelfare() == null) {
                                        JsfUtil.addErrorMessage("Error in Staff Welfare Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getStaffWelfare().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from Staff Welfare.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() == null) {
                                            JsfUtil.addErrorMessage("Please Fill Welfare Staff Name");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;

                                case ewallet:
                                    if (cd.getPaymentMethodData().getEwallet() == null) {
                                        JsfUtil.addErrorMessage("Error in eWallet payment Details");
                                        return true;
                                    } else {
                                        checkAmount = cd.getPaymentMethodData().getEwallet().getTotalValue();

                                        if (checkAmount <= 0.0) {
                                            JsfUtil.addErrorMessage("Please enter the amount to be paid from eWallet.");
                                            return true;
                                        }
                                        if (cd.getPaymentMethodData().getEwallet().getInstitution() == null || ((cd.getPaymentMethodData().getEwallet().getReferenceNo() == null || cd.getPaymentMethodData().getEwallet().getReferenceNo().trim().isEmpty()) && (cd.getPaymentMethodData().getEwallet().getNo() == null || cd.getPaymentMethodData().getEwallet().getNo().trim().isEmpty()))) {
                                            JsfUtil.addErrorMessage("Please Fill eWallet Reference Number and Bank.");
                                            return true;
                                        }
                                        multipleTotal += checkAmount;
                                    }
                                    break;
                                default:
                                    JsfUtil.addErrorMessage("Payment method " + cd.getPaymentMethod() + " is not supported for post final bill payments.");
                                    return true;
                            }
                        }
                    }

                    if (multipleTotal <= 0.0) {
                        JsfUtil.addErrorMessage("Please enter valid payment amounts");
                        return true;
                    }
                    amountToCheck = multipleTotal;
                    break;
                default:
                    JsfUtil.addErrorMessage("Payment method " + method + " is not supported for post final bill payments.");
                    return true;
            }
        }

        getCurrent().setTotal(amountToCheck);
        getCurrent().setNetTotal(amountToCheck);

        return false;
    }

    public void pay() {
        if (errorCheck()) {
            return;
        }

        saveBill();
        saveBillItem();

        paymentService.createPayment(
                current,
                current.getPaymentMethod(),
                paymentMethodData,
                sessionController.getInstitution(),
                sessionController.getDepartment(),
                sessionController.getLoggedUser());

        JsfUtil.addSuccessMessage("Payment Bill Saved");
        paymentMethod = null;
        printPreview = true;
    }

    private void saveBill() {
        getCurrent().setInstitution(getSessionController().getInstitution());
        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setBillType(BillType.PostFinalBillInwardPayment);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT);
        getCurrent().setPaymentMethod(paymentMethod);
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPFP));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getCurrent().getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWPFP));
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setPatient(getCurrent().getPatientEncounter().getPatient());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());
        getCurrent().setComments(comment);

        if (getCurrent().getId() == null) {
            getBilledBillFacade().create(getCurrent());
        }
    }

    private void saveBillItem() {
        BillItem temBi = new BillItem();
        temBi.setBill(getCurrent());
        temBi.setGrossValue(getCurrent().getTotal());
        temBi.setNetValue(getCurrent().getTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(getSessionController().getLoggedUser());

        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }
    }

    /**
     * Cancel a previously recorded post final bill inward payment. Modeled
     * on {@code InwardSearch.cancelDepositBillPayment()} but WITHOUT the
     * deposit-specific "enough value to cancel" guard (that guard checks
     * pre-final due, which does not apply here - cancelling simply
     * increases the live balance due back up).
     */
    public void cancelPostFinalPayment() {
        if (getCurrent() == null || getCurrent().getId() == null || getCurrent().getId() == 0) {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

        if (getCurrent().getCheckedBy() != null) {
            JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
            return;
        }

        if (getCurrent().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return;
        }

        if (getCurrent().isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded. Can not cancel.");
            return;
        }

        CancelledBill cb = createCancelPostFinalPaymentBill();

        getBillFacade().create(cb);
        cancelBillItems(cb);
        getCurrent().setCancelled(true);
        getCurrent().setCancelledBill(cb);
        getBilledBillFacade().edit(getCurrent());

        paymentService.createPayment(
                cb,
                cb.getPaymentMethod(),
                getPaymentMethodData(),
                sessionController.getInstitution(),
                sessionController.getDepartment(),
                sessionController.getLoggedUser());

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        JsfUtil.addSuccessMessage("Cancelled");

        printPreview = true;
    }

    private CancelledBill createCancelPostFinalPaymentBill() {
        CancelledBill cb = new CancelledBill();
        cb.copy(getCurrent());
        cb.setBackwardReferenceBill(getCurrent());
        cb.setBillType(BillType.PostFinalBillInwardPayment);
        cb.setBillTypeAtomic(BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT_CANCELLATION);
        cb.setInstitution(getSessionController().getInstitution());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.CAN));
        cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.CAN));
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setTotal(0 - getCurrent().getTotal());
        cb.setNetTotal(0 - getCurrent().getNetTotal());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        return cb;
    }

    private void cancelBillItems(CancelledBill cb) {
        String sql = "Select bi From BillItem bi where bi.retired=false and bi.bill=:bill";
        HashMap hm = new HashMap();
        hm.put("bill", getCurrent());
        List<BillItem> billItems = getBillItemFacade().findByJpql(sql, hm);
        for (BillItem bi : billItems) {
            BillItem cbi = new BillItem();
            cbi.setBill(cb);
            cbi.copy(bi);
            cbi.invertValue(bi);
            cbi.setCreatedAt(new Date());
            cbi.setCreater(getSessionController().getLoggedUser());
            if (cbi.getId() == null) {
                getBillItemFacade().create(cbi);
            }
        }
    }

    /**
     * Refund a previously recorded post final bill inward payment. Modeled
     * on {@code InwardRefundController.pay()}/{@code saveBill()} but with
     * the post-final BillType/BillTypeAtomic and the generic INWREF
     * numbering suffix (already used by InwardRefundController for
     * deposit refunds - not a deposit-specific suffix). Uses a dedicated
     * {@link RefundBill} instance ({@link #getRefundCurrent()}) rather than
     * {@link #getCurrent()}, since the refund bill being created is a
     * distinct entity from the (already saved) payment bill it refunds.
     */
    public void refundPostFinalPayment() {
        if (errorCheckForRefund()) {
            return;
        }

        saveRefundBill();
        getRefundCurrent().setBillTypeAtomic(BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT_REFUND);
        getBillFacade().edit(getRefundCurrent());
        saveRefundBillItem();

        getOriginalBillToRefund().setRefunded(true);
        getBillFacade().edit(getOriginalBillToRefund());

        printPreview = true;

        List<Payment> payments = paymentService.createPayment(getRefundCurrent(), paymentMethodData);
        paymentService.updateBalances(payments);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getRefundCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        JsfUtil.addSuccessMessage("Payment Bill Saved");
    }

    /**
     * Navigate to the post final bill inward payment refund page, requiring
     * an active shift. Mirrors {@code InwardRefundController.navigateToInwardDepositRefund()}.
     */
    public String navigateToPostFinalPaymentRefund() {
        refundCurrent = null;
        paidAmount = 0.0;
        printPreview = false;
        eligiblePostFinalPaymentBills = null;
        originalBillToRefund = null;
        remainingRefundableAmountCache = null;
        financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
        if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
            JsfUtil.addErrorMessage("Start Your Shift First !");
            return "/cashier/index?faces-redirect=true";
        }
        return "/inward/inward_bill_post_final_payment_refund?faces-redirect=true";
    }

    /**
     * After the cashier picks a BHT on the refund page, compute how much is
     * available to refund: the sum of non-cancelled post-final payments for
     * this encounter (same live sum {@link #getLiveBalanceDue()} subtracts
     * off, exposed here directly since refunding isn't capped by the final
     * bill's balance - it's capped by how much was actually paid post-final).
     * Mirrors {@code InwardRefundController.calculatePaidAmount()}.
     */
    public void selectBhtListenerForRefund() {
        if (getRefundCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return;
        }
        paidAmount = getPostFinalPaymentTotal(getRefundCurrent().getPatientEncounter());
        loadEligiblePostFinalPaymentBills();
    }

    /**
     * Loads the encounter's post-final payment bills that still have a
     * nonzero remaining refundable amount (issue #22627 - refund must be
     * linked to one specific original bill, not an encounter-wide
     * aggregate). Mirrors {@code InwardRefundController.loadEligiblePaymentBills()}.
     */
    private void loadEligiblePostFinalPaymentBills() {
        eligiblePostFinalPaymentBills = new ArrayList<>();
        originalBillToRefund = null;
        remainingRefundableAmountCache = new HashMap<>();
        for (Bill b : getInwardBean().fetchPostFinalPaymentBill(getRefundCurrent().getPatientEncounter(), null)) {
            if (b instanceof BilledBill && !b.isCancelled() && computeRemainingRefundableAmount(b) > 0.01) {
                eligiblePostFinalPaymentBills.add(b);
            }
        }
    }

    /**
     * Cached per-bill remaining-refundable amount, populated once per bill
     * by loadEligiblePostFinalPaymentBills(). The bill picker table calls
     * this once per row through EL on every render/postback, so without the
     * cache it would re-run computeRemainingRefundableAmount()'s JPQL SUM
     * query for every row on every render, on top of the same query already
     * run for every bill while building eligiblePostFinalPaymentBills.
     * Mirrors {@code InwardRefundController.getRemainingRefundableAmount()}.
     */
    public double getRemainingRefundableAmount(Bill originalBill) {
        if (originalBill == null) {
            return 0.0;
        }
        if (remainingRefundableAmountCache != null && originalBill.getId() != null) {
            Double cached = remainingRefundableAmountCache.get(originalBill.getId());
            if (cached != null) {
                return cached;
            }
        }
        return computeRemainingRefundableAmount(originalBill);
    }

    /**
     * Sum of netTotal of every RefundBill already linked to originalBill via
     * referenceBill (always <= 0), added to the bill's own netTotal. Do NOT
     * use Bill.refundBills/getRefundBills() here - that collection is
     * mappedBy="billedBill", which no refund flow in this codebase
     * populates, so it never reflects referenceBill-linked refunds.
     */
    private double computeRemainingRefundableAmount(Bill originalBill) {
        String sql = "select sum(b.netTotal) from Bill b where b.referenceBill=:orig and b.retired=false";
        HashMap hm = new HashMap();
        hm.put("orig", originalBill);
        double refundedSoFar = getBillFacade().findDoubleByJpql(sql, hm);
        double remaining = originalBill.getNetTotal() + refundedSoFar;
        if (remainingRefundableAmountCache != null && originalBill.getId() != null) {
            remainingRefundableAmountCache.put(originalBill.getId(), remaining);
        }
        return remaining;
    }

    public void selectBillToRefundListener() {
        if (getOriginalBillToRefund() == null) {
            JsfUtil.addErrorMessage("Select a Payment Bill to Refund");
            return;
        }
        getRefundCurrent().setTotal(getRemainingRefundableAmount(getOriginalBillToRefund()));
    }

    /**
     * Lets the cashier pick a different bill without losing the BHT
     * selection or re-querying eligiblePostFinalPaymentBills.
     */
    public void clearSelectedBillToRefund() {
        originalBillToRefund = null;
        getRefundCurrent().setTotal(0);
    }

    public List<Bill> getEligiblePostFinalPaymentBills() {
        if (eligiblePostFinalPaymentBills == null) {
            eligiblePostFinalPaymentBills = new ArrayList<>();
        }
        return eligiblePostFinalPaymentBills;
    }

    public void setEligiblePostFinalPaymentBills(List<Bill> eligiblePostFinalPaymentBills) {
        this.eligiblePostFinalPaymentBills = eligiblePostFinalPaymentBills;
    }

    public Bill getOriginalBillToRefund() {
        return originalBillToRefund;
    }

    public void setOriginalBillToRefund(Bill originalBillToRefund) {
        this.originalBillToRefund = originalBillToRefund;
    }

    private boolean errorCheckForRefund() {
        if (getRefundCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return true;
        }

        if (getOriginalBillToRefund() == null) {
            JsfUtil.addErrorMessage("Select a Payment Bill to Refund");
            return true;
        }

        if (getRefundCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getRefundCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        double remaining = getRemainingRefundableAmount(getOriginalBillToRefund());

        if (Math.abs(remaining) < getRefundCurrent().getTotal()) {
            double different = Math.abs(Math.abs(remaining) - Math.abs(getRefundCurrent().getTotal()));
            if (different > 0.1) {
                JsfUtil.addErrorMessage("Check Refunding Amount");
                return true;
            }
        }

        return false;
    }

    private void saveRefundBill() {
        getBillBean().setPaymentMethodData(getRefundCurrent(), getRefundCurrent().getPaymentMethod(), getPaymentMethodData());
        getRefundCurrent().setBillType(BillType.PostFinalBillInwardPayment);
        getRefundCurrent().setBillDate(new Date());
        getRefundCurrent().setBillTime(new Date());
        getRefundCurrent().setInstitution(getSessionController().getInstitution());
        getRefundCurrent().setDepartment(getSessionController().getDepartment());
        getRefundCurrent().setReferenceBill(getOriginalBillToRefund());
        getRefundCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getRefundCurrent().getBillType(), BillClassType.RefundBill, BillNumberSuffix.INWREF));
        getRefundCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getRefundCurrent().getBillType(), BillClassType.RefundBill, BillNumberSuffix.INWREF));

        double dbl = Math.abs(getRefundCurrent().getTotal());

        getRefundCurrent().setTotal(0 - dbl);
        getRefundCurrent().setNetTotal(0 - dbl);
        getRefundCurrent().setCreatedAt(new Date());
        getRefundCurrent().setCreater(getSessionController().getLoggedUser());

        if (getRefundCurrent().getId() == null) {
            getBillFacade().create(getRefundCurrent());
        }
    }

    private void saveRefundBillItem() {
        BillItem temBi = new BillItem();
        temBi.setBill(getRefundCurrent());
        temBi.setGrossValue(0 - getRefundCurrent().getTotal());
        temBi.setNetValue(0 - getRefundCurrent().getTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(getSessionController().getLoggedUser());

        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    /**
     * NOTE: unlike InwardPaymentController, this flow does not support
     * PaymentMethod.PatientDeposit (the new controller was deliberately
     * scoped, per the implementation brief, without PatientDepositController
     * / PatientDepositService dependencies) - kept as a no-op hook for API
     * parity with InwardPaymentController.
     */
    public void updatePaymentData() {
    }

    public void changePaymentMethodChange() {
        if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().clear();
        } else {
            listnerForPaymentMethodChange();
        }
    }

    public void listnerForPaymentMethodChange() {
        if (getPaymentMethod() == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(getCurrent().getTotal());
        }
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void makeNull() {
        current = null;
        printPreview = false;
        comment = null;
        paymentMethod = null;
        total = 0.0;
        paymentMethodData = null;
    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {
        double multiplePaymentMethodTotalValue = 0.0;
        if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

            if (paymentMethodData != null && paymentMethodData.getPaymentMethodMultiple() != null && paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd == null || cd.getPaymentMethodData() == null) {
                        continue;
                    }
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                    multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
                }
            }
            getCurrent().setTotal(multiplePaymentMethodTotalValue);
            getCurrent().setNetTotal(multiplePaymentMethodTotalValue);
        }
        return multiplePaymentMethodTotalValue;
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        calculatRemainForMultiplePaymentTotal();
        if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {

            if (paymentMethodData == null
                    || paymentMethodData.getPaymentMethodMultiple() == null
                    || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                    || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
                return;
            }

            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);

            switch (pm.getPaymentMethod()) {
                case Cash:
                    if (pm.getPaymentMethodData().getCash().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCash().setTotalValue(0.0);
                    }
                    break;
                case Card:
                    if (pm.getPaymentMethodData().getCreditCard().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCreditCard().setTotalValue(0.0);
                    }
                    break;
                case Cheque:
                    if (pm.getPaymentMethodData().getCheque().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCheque().setTotalValue(0.0);
                    }
                    break;
                case Slip:
                    if (pm.getPaymentMethodData().getSlip().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getSlip().setTotalValue(0.0);
                    }
                    break;
                case ewallet:
                    if (pm.getPaymentMethodData().getEwallet().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getEwallet().setTotalValue(0.0);
                    }
                    break;
                case Credit:
                    if (pm.getPaymentMethodData().getCredit().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCredit().setTotalValue(0.0);
                    }
                    break;
                case Staff:
                    if (pm.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getStaffCredit().setTotalValue(0.0);
                    }
                    break;
                case Staff_Welfare:
                    if (pm.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getStaffWelfare().setTotalValue(0.0);
                    }
                    break;
                case OnlineSettlement:
                    if (pm.getPaymentMethodData().getOnlineSettlement().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(0.0);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected value: " + pm.getPaymentMethod());
            }
        }
    }

    @Override
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null
                || paymentMethodData == null
                || paymentMethodData.getPaymentMethodMultiple() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        List<ComponentDetail> details = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();
        int lastIndex = details.size() - 1;
        int currentIndex = details.indexOf(cd);
        return currentIndex != -1 && currentIndex == lastIndex;
    }

    public BilledBillFacade getBilledBillFacade() {
        return billedBillFacade;
    }

    public void setBilledBillFacade(BilledBillFacade billedBillFacade) {
        this.billedBillFacade = billedBillFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public RefundBill getRefundCurrent() {
        if (refundCurrent == null) {
            refundCurrent = new RefundBill();
            refundCurrent.setBillType(BillType.PostFinalBillInwardPayment);
        }
        return refundCurrent;
    }

    public void setRefundCurrent(RefundBill refundCurrent) {
        this.refundCurrent = refundCurrent;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BilledBill getCurrent() {
        if (current == null) {
            current = new BilledBill();
            current.setBillType(BillType.PostFinalBillInwardPayment);
        }
        return current;
    }

    public void setCurrent(BilledBill current) {
        this.current = current;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

}
