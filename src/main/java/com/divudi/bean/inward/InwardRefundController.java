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
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.*;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.core.entity.*;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.service.PaymentService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InwardRefundController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    PaymentService paymentService;
    @Inject
    private SessionController sessionController;
    @Inject
    private FinancialTransactionController financialTransactionController;
    private double paidAmount;
    double netTotal;
    private Bill current;
    private PaymentMethodData paymentMethodData;
    @EJB
    private BillNumberGenerator billNumberBean;
    private boolean printPreview;
    @Inject
    private InwardBeanController inwardBean;
    private List<Bill> eligiblePaymentBills;
    private Bill originalBillToRefund;
    private Map<Long, Double> remainingRefundableAmountCache;

    public void makeNull() {
        current = null;
        paidAmount = 0.0;
        printPreview = false;
        eligiblePaymentBills = null;
        originalBillToRefund = null;
        remainingRefundableAmountCache = null;
    }

    /**
     * Navigate to the inward deposit refund page, requiring an active shift.
     * If the logged user has no open shift start fund bill, show an error and
     * send them to the cashier index to start a shift first.
     */
    public String navigateToInwardDepositRefund() {
        makeNull();
        financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
        if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
            // Use Flash scope to preserve error message across redirect
            JsfUtil.addErrorMessage("Start Your Shift First !");
            return "/cashier/index?faces-redirect=true";
        }
        return "/inward/inward_bill_refund?faces-redirect=true";
    }

    /**
     * Navigate to the inward deposit refund page with a specific payment bill
     * pre-selected, for the "Refund" button on the Payment Reprint view page
     * (issue #22820). Reuses the same eligibility filtering as the manual
     * bill picker (loadEligiblePaymentBills()) so this can't be tricked into
     * pre-selecting an ineligible (cancelled / fully refunded) bill.
     */
    public String navigateToRefundFromPaymentBill(Bill originPaymentBill) {
        makeNull();
        if (originPaymentBill == null || originPaymentBill.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return "";
        }
        financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
        if (financialTransactionController.getNonClosedShiftStartFundBill() == null) {
            JsfUtil.addErrorMessage("Start Your Shift First !");
            return "/cashier/index?faces-redirect=true";
        }
        getCurrent().setPatientEncounter(originPaymentBill.getPatientEncounter());
        loadEligiblePaymentBills();
        if (!getEligiblePaymentBills().contains(originPaymentBill)) {
            JsfUtil.addErrorMessage("This bill is not eligible for refund (already cancelled or fully refunded).");
            return "";
        }
        originalBillToRefund = originPaymentBill;
        selectBillToRefundListener();
        return "/inward/inward_bill_refund?faces-redirect=true";
    }

    public PaymentMethod[] getPaymentMethods() {
        return PaymentMethod.values();
    }

    @Inject
    private PaymentSchemeController paymentSchemeController;

    private boolean errorCheck() {
        if (getCurrent().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("Select BHT");
            return true;
        }

        if (getOriginalBillToRefund() == null) {
            JsfUtil.addErrorMessage("Select a Payment Bill to Refund");
            return true;
        }

        if (getOriginalBillToRefund().isCancelled()) {
            JsfUtil.addErrorMessage("This bill has been cancelled and cannot be refunded.");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        double remaining = getRemainingRefundableAmount(getOriginalBillToRefund());

        if (Math.abs(remaining) < getCurrent().getTotal()) {
            double different = Math.abs(Math.abs(remaining) - Math.abs(getCurrent().getTotal()));

            if (different > 0.1) {
                JsfUtil.addErrorMessage("Check Refuning Amount");
                return true;
            }
        }

        return false;
    }

    @EJB
    private CashTransactionBean cashTransactionBean;

    public void pay() {
        if (errorCheck()) {
            return;
        }

        saveBill();
        getCurrent().setBillTypeAtomic(BillTypeAtomic.INWARD_PAYMENT_REFUND);
        getBillFacade().edit(getCurrent());
        saveBillItem();

        getOriginalBillToRefund().setRefunded(true);
        getOriginalBillToRefund().setRefundedBill(getCurrent());
        getBillFacade().edit(getOriginalBillToRefund());

        printPreview = true;

        List<Payment> payments = paymentService.createPayment(getCurrent(), paymentMethodData);
        paymentService.updateBalances(payments);

        getBillBean().updateInwardDipositList(getCurrent().getPatientEncounter(), getCurrent());

        if (getCurrent().getPatientEncounter().isPaymentFinalized()) {
            getInwardBean().updateFinalFill(getCurrent().getPatientEncounter());
            if (getCurrent().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                getInwardBean().updateCreditDetail(getCurrent().getPatientEncounter(), getCurrent().getPatientEncounter().getFinalBill().getNetTotal());
            }
        }

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        JsfUtil.addSuccessMessage("Payment Bill Saved");
    }

    @Inject
    private BillBeanController billBean;

    private void saveBill() {
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        getCurrent().setBillType(BillType.InwardPaymentBill);
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setInstitution(getSessionController().getInstitution());
        getCurrent().setDepartment(getSessionController().getDepartment());
        getCurrent().setReferenceBill(getOriginalBillToRefund());
        getCurrent().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getCurrent().getBillType(), BillClassType.RefundBill, BillNumberSuffix.INWREF));
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getCurrent().getBillType(), BillClassType.RefundBill, BillNumberSuffix.INWREF));

        double dbl = Math.abs(getCurrent().getTotal());

        getCurrent().setTotal(0 - dbl);
        getCurrent().setNetTotal(0 - dbl);
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        }

    }

    private void saveBillItem() {
        BillItem temBi = new BillItem();
        temBi.setBill(getCurrent());
        temBi.setGrossValue(0 - getCurrent().getTotal());
        temBi.setNetValue(0 - getCurrent().getTotal());
        temBi.setCreatedAt(new Date());
        temBi.setCreater(getSessionController().getLoggedUser());

        if (temBi.getId() == null) {
            getBillItemFacade().create(temBi);
        }

        saveBillFee(temBi);

    }

    private void saveBillFee(BillItem bt) {
        BillFee bf = new BillFee();
        bf.setBill(getCurrent());
        bf.setBillItem(bt);
        bf.setCreatedAt(new Date());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setFeeGrossValue(0 - getCurrent().getTotal());
        bf.setFeeValue(0 - getCurrent().getTotal());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
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

    public Bill getCurrent() {
        if (current == null) {
            current = new RefundBill();
            current.setBillType(BillType.InwardPaymentBill);
        }

        return current;
    }

    public void setCurrent(RefundBill current) {
        this.current = current;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public void calculatePaidAmount() {

        HashMap map = new HashMap();
        String sql = "SELECT sum(bb.netTotal) FROM Bill bb where bb.retired=false "
                + " and bb.billType=:bType and bb.patientEncounter=:pe";
        map.put("bType", BillType.InwardPaymentBill);
        map.put("pe", getCurrent().getPatientEncounter());
        paidAmount = getBillFacade().findDoubleByJpql(sql, map);

    }

    public void selectBhtListener() {
        if (getCurrent().getPatientEncounter().isPaymentFinalized()) {
            calculteFinalBillMax();
        } else {
            calculatePaidAmount();
        }
        loadEligiblePaymentBills();
    }

    /**
     * Loads the encounter's payment bills that still have a nonzero
     * remaining refundable amount (issue #22627 - refund must be linked to
     * one specific original bill, not an encounter-wide aggregate).
     */
    private void loadEligiblePaymentBills() {
        eligiblePaymentBills = new ArrayList<>();
        originalBillToRefund = null;
        remainingRefundableAmountCache = new HashMap<>();
        for (Bill b : getInwardBean().fetchPaymentBill(getCurrent().getPatientEncounter(), null)) {
            if (b instanceof BilledBill && !b.isCancelled() && computeRemainingRefundableAmount(b) > 0.01) {
                eligiblePaymentBills.add(b);
            }
        }
    }

    /**
     * Cached per-bill remaining-refundable amount, populated once per bill
     * by loadEligiblePaymentBills(). The bill picker table calls this once
     * per row through EL on every render/postback, so without the cache it
     * would re-run computeRemainingRefundableAmount()'s JPQL SUM query for
     * every row on every render, on top of the same query already run for
     * every bill while building eligiblePaymentBills.
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
     * mappedBy="billedBill", which no refund flow in this codebase populates,
     * so it never reflects referenceBill-linked refunds.
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
        getCurrent().setTotal(getRemainingRefundableAmount(getOriginalBillToRefund()));
    }

    /**
     * Lets the cashier pick a different bill without losing the BHT
     * selection or re-querying eligiblePaymentBills.
     */
    public void clearSelectedBillToRefund() {
        originalBillToRefund = null;
        getCurrent().setTotal(0);
    }

    public List<Bill> getEligiblePaymentBills() {
        if (eligiblePaymentBills == null) {
            eligiblePaymentBills = new ArrayList<>();
        }
        return eligiblePaymentBills;
    }

    public void setEligiblePaymentBills(List<Bill> eligiblePaymentBills) {
        this.eligiblePaymentBills = eligiblePaymentBills;
    }

    public Bill getOriginalBillToRefund() {
        return originalBillToRefund;
    }

    public void setOriginalBillToRefund(Bill originalBillToRefund) {
        this.originalBillToRefund = originalBillToRefund;
    }

    public void calculteFinalBillMax() {
        String sql = "Select b From BilledBill b where"
                + " b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.confirmedFinalBill=true "
                + " and b.patientEncounter=:pe"
                + " order by b.id desc";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);
        hm.put("pe", getCurrent().getPatientEncounter());

        Bill b = getBillFacade().findFirstByJpql(sql, hm);

        if (b == null) {
            paidAmount = 0;
            netTotal = 0;
            return;
        }

        paidAmount = b.getNetTotal() - (b.getPaidAmount() + getCurrent().getPatientEncounter().getCreditPaidAmount());
        netTotal = b.getNetTotal();

//        double paidByPatient = Math.abs(b.getPaidAmount());
//
//        if (getCurrent().getPatientEncounter().getPaymentMethod() == PaymentMethod.Cash) {
//            paidAmount = paidByPatient - Math.abs(b.getNetTotal());
//            return;
//        }
//
//        double creditUsedAmount = Math.abs(getCurrent().getPatientEncounter().getCreditUsedAmount());
//        double creditPaidAmount = Math.abs(getCurrent().getPatientEncounter().getCreditPaidAmount());
//        double netCredit = creditUsedAmount - creditPaidAmount;
//
//        if (getCurrent().getPatientEncounter().getCreditLimit() != 0) {
//            paidAmount = (paidByPatient + netCredit) - Math.abs(b.getNetTotal());
//        }
    }

    public double getPaidAmount() {

        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
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

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
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

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }
}
