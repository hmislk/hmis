package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;

import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.entity.Staff;

import com.divudi.entity.cashTransaction.Drawer;

import com.divudi.facade.BillFacade;
import com.divudi.service.DrawerService;
import com.divudi.service.PaymentService;
import com.divudi.service.ProfessionalPaymentService;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Damith Deshan
 */
@Named
@SessionScoped
public class BillReturnController implements Serializable, ControllerWithMultiplePayments {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillFacade billFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    PaymentService paymentService;
    @EJB
    DrawerService drawerService;
    @EJB
    ProfessionalPaymentService professionalPaymentService;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    BillController billController;
    @Inject
    BillBeanController billBeanController;
    @Inject
    BillItemController billItemController;
    @Inject
    BillFeeController billFeeController;
    @Inject
    DrawerController drawerController;
    @Inject
    AgentAndCcApplicationController agentAndCcApplicationController;

    private ConfigOptionApplicationController configOptionApplicationController;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variable">
    private Staff toStaff;
    private Bill originalBillToReturn;
    private List<BillItem> originalBillItemsAvailableToReturn;
    private List<BillItem> originalBillItemsToSelectedToReturn;

    private Bill newlyReturnedBill;
    private List<BillItem> newlyReturnedBillItems;
    private List<BillFee> newlyReturnedBillFees;
    private List<Payment> returningBillPayments;

    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;
    private boolean returningStarted = false;

    private double refundingTotalAmount;
    private String refundComment;
    private boolean selectAll;

    private PaymentMethodData paymentMethodData;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    public String navigateToReturnOpdBill() {
        if (originalBillToReturn == null) {
            return null;
        }
        originalBillItemsAvailableToReturn = billBeanController.fetchBillItems(originalBillToReturn);
        returningStarted = false;
        paymentMethod = originalBillToReturn.getPaymentMethod();
        paymentMethods = paymentService.fetchAvailablePaymentMethodsForRefundsAndCancellations(originalBillToReturn);
        return "/opd/bill_return?faces-redirect=true";
    }

    public String navigateToReturnCCBill() {
        if (originalBillToReturn == null) {
            return null;
        }

        //System.out.println("Original Bill= " + originalBillToReturn);
        originalBillItemsAvailableToReturn = billBeanController.fetchBillItems(originalBillToReturn);
        //System.out.println("Bill Items Available To Return = " + originalBillItemsAvailableToReturn.size());
        returningStarted = false;
        paymentMethod = originalBillToReturn.getPaymentMethod();
        System.out.println("Method = " + paymentMethod);
        return "/collecting_centre/bill_return?faces-redirect=true";
    }

    public String navigateToOPDBillSearchFormRefundOpdBillView() {
        return "/opd/opd_bill_search?faces-redirect=true";
    }

    public String navigateToRefundBillViewFormOPDBillSearch() {
        return "/opd/bill_return_print?faces-redirect=true";
    }

    public String navigateToRefundCCBillViewFormCCBillSearch() {
        return "/opd/bill_return_print?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Method">
    public BillReturnController() {
    }

    public void makeNull() {

    }

    public void clear() {
        refundComment = null;
        refundingTotalAmount = 0.0;
        returningBillPayments = null;
        newlyReturnedBillFees = null;
        newlyReturnedBillItems = null;
        originalBillItemsToSelectedToReturn = null;
        selectAll = true;
    }

    public void selectAllItems() {
        originalBillItemsToSelectedToReturn = new ArrayList();
        for (BillItem selectedBillItemToReturn : originalBillItemsAvailableToReturn) {
            if (!selectedBillItemToReturn.isRefunded()) {
                originalBillItemsToSelectedToReturn.add(selectedBillItemToReturn);
            }
        }
        calculateRefundingAmount();
        selectAll = false;
    }

    public void unSelectAllItems() {
        originalBillItemsToSelectedToReturn = new ArrayList();
        refundingTotalAmount = 0.0;
        selectAll = true;
    }

    public boolean checkCanReturnBill(Bill bill) {
        List<BillItem> items = billBeanController.fetchBillItems(bill);
        boolean canReturn = false;
        for (BillItem bllItem : items) {
            if (!bllItem.isRefunded()) {
                canReturn = true;
            }
        }
        return canReturn;
    }

    public boolean checkDraverBalance(Drawer drawer, PaymentMethod paymentMethod) {
        boolean canReturn = false;

        switch (paymentMethod) {
            case Cash:
                if (drawer.getCashInHandValue() != null) {
                    if (drawer.getCashInHandValue() < refundingTotalAmount) {
                        canReturn = false;
                    } else {
                        canReturn = true;
                    }
                } else {
                    canReturn = false;
                }
                break;
            case Card:
                canReturn = true;
                break;
            case MultiplePaymentMethods:
                canReturn = true;
                break;
            case Staff:
                canReturn = true;
                break;
            case Credit:
                canReturn = true;
                break;
            case Staff_Welfare:
                canReturn = true;
                break;
            case Cheque:
                canReturn = true;
                break;
            case Slip:
                canReturn = true;
                break;
            case OnlineSettlement:
                canReturn = true;
                break;
            case PatientDeposit:
                canReturn = true;
                break;
            default:
                break;
        }
        if (!configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
            canReturn = true;
        }
        return canReturn;
    }

    public String settleOpdReturnBill() {
        if (returningStarted) {
            JsfUtil.addErrorMessage("Already Returning Started");
            return null;
        }
        if (originalBillToReturn == null) {
            JsfUtil.addErrorMessage("Already Returning Started");
            returningStarted = false;
            return null;
        }
        if (originalBillItemsToSelectedToReturn == null || originalBillItemsToSelectedToReturn.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected to return");
            returningStarted = false;
            return null;
        }

        if (refundComment == null || refundComment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter Refund Comment");
            returningStarted = false;
            return null;
        }
        calculateRefundingAmount();

        Drawer loggedUserDraver = drawerController.getUsersDrawer(sessionController.getLoggedUser());

        if (!drawerService.hasSufficientDrawerBalance(loggedUserDraver, paymentMethod, refundingTotalAmount)) {
            JsfUtil.addErrorMessage("Your Draver does not have enough Money");
            returningStarted = false;
            return null;
        }

        originalBillToReturn = billFacade.findWithoutCache(originalBillToReturn.getId());

        if (originalBillToReturn.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            returningStarted = false;
            return null;
        }

        for (BillItem bi : originalBillItemsToSelectedToReturn) {
            if (professionalPaymentService.isProfessionalFeePaid(originalBillToReturn, bi)) {
                JsfUtil.addErrorMessage("Staff or Outside Institute fees have already been paid for the " + bi.getItem().getName() + " procedure.");
                return null;
            }
        }

        //TO DO: Check weather selected items is refunded
        if (!checkCanReturnBill(originalBillToReturn)) {
            JsfUtil.addErrorMessage("All Items are Already Refunded");
            returningStarted = false;
            return null;
        }

        // fetch original bill now, checked alteady returned, cancelled, ,
        newlyReturnedBill = new RefundBill();
        newlyReturnedBill.copy(originalBillToReturn);
        newlyReturnedBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_REFUND);
        newlyReturnedBill.setComments(refundComment);
        newlyReturnedBill.setInstitution(sessionController.getInstitution());
        newlyReturnedBill.setDepartment(sessionController.getDepartment());
        newlyReturnedBill.setReferenceBill(originalBillToReturn);
        newlyReturnedBill.invertValueOfThisBill();

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPD_BILL_REFUND);
        newlyReturnedBill.setDeptId(deptId);
        newlyReturnedBill.setInsId(deptId);
        billController.save(newlyReturnedBill);

        List<Bill> refundBillList = originalBillToReturn.getRefundBills();
        refundBillList.add(newlyReturnedBill);
        originalBillToReturn.setRefunded(true);
        originalBillToReturn.setRefundBills(refundBillList);

        billController.save(originalBillToReturn);
        double returningTotal = 0.0;
        double returningNetTotal = 0.0;
        double returningHospitalTotal = 0.0;
        double returningStaffTotal = 0.0;
        double returningReagentTotal = 0.0;
        double returningOtherTotal = 0.0;
        double returningDiscount = 0.0;

        newlyReturnedBillItems = new ArrayList<>();
        returningBillPayments = new ArrayList<>();
        newlyReturnedBillFees = new ArrayList<>();

        for (BillItem selectedBillItemToReturn : originalBillItemsToSelectedToReturn) {

            returningTotal += selectedBillItemToReturn.getGrossValue();
            returningNetTotal += selectedBillItemToReturn.getNetValue();
            returningHospitalTotal += selectedBillItemToReturn.getHospitalFee();
            returningStaffTotal += selectedBillItemToReturn.getStaffFee();
            returningReagentTotal += selectedBillItemToReturn.getReagentFee();
            returningOtherTotal += selectedBillItemToReturn.getOtherFee();
            returningDiscount += selectedBillItemToReturn.getDiscount();

            BillItem newlyCreatedReturningItem = new BillItem();
            newlyCreatedReturningItem.copy(selectedBillItemToReturn);
            newlyCreatedReturningItem.invertValue();
            newlyCreatedReturningItem.setBill(newlyReturnedBill);
            newlyCreatedReturningItem.setReferanceBillItem(selectedBillItemToReturn);
            billItemController.save(newlyCreatedReturningItem);
            newlyReturnedBillItems.add(newlyCreatedReturningItem);
            selectedBillItemToReturn.setRefunded(true);
            selectedBillItemToReturn.setReferanceBillItem(newlyCreatedReturningItem);
            billItemController.save(selectedBillItemToReturn);
            List<BillFee> originalBillFeesOfSelectedBillItem = billBeanController.fetchBillFees(selectedBillItemToReturn);

            if (originalBillFeesOfSelectedBillItem != null) {
                for (BillFee origianlFee : originalBillFeesOfSelectedBillItem) {
                    BillFee newlyCreatedBillFeeToReturn = new BillFee();
                    newlyCreatedBillFeeToReturn.copy(origianlFee);
                    newlyCreatedBillFeeToReturn.invertValue();
                    newlyCreatedBillFeeToReturn.setBill(newlyReturnedBill);
                    newlyCreatedBillFeeToReturn.setBillItem(newlyCreatedReturningItem);
                    newlyCreatedBillFeeToReturn.setReferenceBillFee(origianlFee);
                    newlyCreatedBillFeeToReturn.setReferenceBillItem(selectedBillItemToReturn);
                    billFeeController.save(newlyCreatedBillFeeToReturn);
                    newlyReturnedBillFees.add(newlyCreatedBillFeeToReturn);

                    origianlFee.setReturned(true);
                    origianlFee.setReferenceBillFee(newlyCreatedBillFeeToReturn);
                    origianlFee.setReferenceBillItem(newlyCreatedReturningItem);
                    billFeeController.save(origianlFee);

                }
            }
        }

        newlyReturnedBill.setGrantTotal(0 - returningTotal);
        newlyReturnedBill.setNetTotal(0 - returningNetTotal);
        newlyReturnedBill.setTotal(0 - returningTotal);
        newlyReturnedBill.setHospitalFee(0 - returningHospitalTotal);
        newlyReturnedBill.setProfessionalFee(0 - returningStaffTotal);
        newlyReturnedBill.setDiscount(0 - returningDiscount);
        billController.save(newlyReturnedBill);

        returningBillPayments = paymentService.createPayment(newlyReturnedBill, getPaymentMethodData());

//        Payment returningPayment = new Payment();
//        returningPayment.setBill(newlyReturnedBill);
//        returningPayment.setPaymentMethod(paymentMethod);
//        returningPayment.setInstitution(sessionController.getInstitution());
//        returningPayment.setDepartment(sessionController.getDepartment());
//        returningPayment.setPaidValue(newlyReturnedBill.getNetTotal());
//        paymentController.save(returningPayment);
//        returningBillPayments.add(returningPayment);
        paymentService.updateBalances(returningBillPayments);

//        if (paymentMethod == PaymentMethod.PatientDeposit) {
//            PatientDeposit pd = patientDepositController.getDepositOfThePatient(newlyReturnedBill.getPatient(), sessionController.getDepartment());
//            patientDepositController.updateBalance(newlyReturnedBill, pd);
//        } else if (paymentMethod == PaymentMethod.Staff_Welfare) {
//            staffBean.updateStaffWelfare(newlyReturnedBill.getToStaff(), -Math.abs(newlyReturnedBill.getNetTotal()));
//            System.out.println("updated = ");
//        }
        // drawer Update
//        drawerController.updateDrawerForOuts(returningPayment);
        returningStarted = false;
        return "/opd/bill_return_print?faces-redirect=true";

    }

    public void calculateRefundingAmount() {
        refundingTotalAmount = 0.0;
        for (BillItem selectedBillItemToReturn : originalBillItemsToSelectedToReturn) {
            refundingTotalAmount += selectedBillItemToReturn.getNetValue();
        }
        if (originalBillItemsToSelectedToReturn.size() == 0) {
            selectAll = true;
        } else {
            selectAll = false;
        }
    }

    public String settleCCReturnBill() {
        if (returningStarted) {
            JsfUtil.addErrorMessage("Already Returning Started");
            return null;
        }
        if (originalBillToReturn == null) {
            JsfUtil.addErrorMessage("Already Returning Started");
            returningStarted = false;
            return null;
        }
        if (originalBillItemsToSelectedToReturn == null || originalBillItemsToSelectedToReturn.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected to return");
            returningStarted = false;
            return null;
        }

        if (refundComment == null || refundComment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter Refund Comment");
            returningStarted = false;
            return null;
        }

        calculateRefundingAmount();

        originalBillToReturn = billFacade.findWithoutCache(originalBillToReturn.getId());
        if (originalBillToReturn.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            returningStarted = false;
            return null;
        }
        //TO DO: Check weather selected items is refunded
        if (!checkCanReturnBill(originalBillToReturn)) {
            JsfUtil.addErrorMessage("All Items are Already Refunded");
            returningStarted = false;
            return null;
        }

        // fetch original bill now, checked alteady returned, cancelled, ,
        newlyReturnedBill = new RefundBill();
        newlyReturnedBill.copy(originalBillToReturn);
        newlyReturnedBill.setBillTypeAtomic(BillTypeAtomic.CC_BILL_REFUND);
        newlyReturnedBill.setComments(refundComment);
        newlyReturnedBill.setInstitution(sessionController.getInstitution());
        newlyReturnedBill.setDepartment(sessionController.getDepartment());
        newlyReturnedBill.setReferenceBill(originalBillToReturn);
        newlyReturnedBill.invertValueOfThisBill();

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.CC_BILL_REFUND);
        newlyReturnedBill.setDeptId(deptId);
        newlyReturnedBill.setInsId(deptId);
        billController.save(newlyReturnedBill);

        List<Bill> refundBillList = originalBillToReturn.getRefundBills();
        refundBillList.add(newlyReturnedBill);
        originalBillToReturn.setRefunded(true);
        originalBillToReturn.setRefundBills(refundBillList);

        billController.save(originalBillToReturn);
        double returningTotal = 0.0;
        double returningNetTotal = 0.0;
        double returningHospitalTotal = 0.0;
        double returningCCTotal = 0.0;
        double returningStaffTotal = 0.0;
        double returningDiscount = 0.0;

        newlyReturnedBillItems = new ArrayList<>();
        returningBillPayments = new ArrayList<>();
        newlyReturnedBillFees = new ArrayList<>();

        for (BillItem selectedBillItemToReturn : originalBillItemsToSelectedToReturn) {

            returningTotal += selectedBillItemToReturn.getGrossValue();
            returningNetTotal += selectedBillItemToReturn.getNetValue();
            returningHospitalTotal += selectedBillItemToReturn.getHospitalFee();
            returningCCTotal += selectedBillItemToReturn.getCollectingCentreFee();
            returningStaffTotal += selectedBillItemToReturn.getStaffFee();
            returningDiscount += selectedBillItemToReturn.getDiscount();

            BillItem newlyCreatedReturningItem = new BillItem();
            newlyCreatedReturningItem.copy(selectedBillItemToReturn);
            newlyCreatedReturningItem.invertValue();
            newlyCreatedReturningItem.setBill(newlyReturnedBill);
            newlyCreatedReturningItem.setReferanceBillItem(selectedBillItemToReturn);
            billItemController.save(newlyCreatedReturningItem);
            newlyReturnedBillItems.add(newlyCreatedReturningItem);
            selectedBillItemToReturn.setRefunded(true);
            selectedBillItemToReturn.setReferanceBillItem(newlyCreatedReturningItem);
            billItemController.save(selectedBillItemToReturn);
            List<BillFee> originalBillFeesOfSelectedBillItem = billBeanController.fetchBillFees(selectedBillItemToReturn);

            if (originalBillFeesOfSelectedBillItem != null) {
                for (BillFee origianlFee : originalBillFeesOfSelectedBillItem) {
                    BillFee newlyCreatedBillFeeToReturn = new BillFee();
                    newlyCreatedBillFeeToReturn.copy(origianlFee);
                    newlyCreatedBillFeeToReturn.invertValue();
                    newlyCreatedBillFeeToReturn.setBill(newlyReturnedBill);
                    newlyCreatedBillFeeToReturn.setBillItem(newlyCreatedReturningItem);
                    newlyCreatedBillFeeToReturn.setReferenceBillFee(origianlFee);
                    newlyCreatedBillFeeToReturn.setReferenceBillItem(selectedBillItemToReturn);
                    billFeeController.save(newlyCreatedBillFeeToReturn);
                    newlyReturnedBillFees.add(newlyCreatedBillFeeToReturn);

                    origianlFee.setReturned(true);
                    origianlFee.setReferenceBillFee(newlyCreatedBillFeeToReturn);
                    origianlFee.setReferenceBillItem(newlyCreatedReturningItem);
                    billFeeController.save(origianlFee);

                }
            }
        }

        newlyReturnedBill.setGrantTotal(0 - returningTotal);
        newlyReturnedBill.setNetTotal(0 - returningNetTotal);
        newlyReturnedBill.setTotal(0 - returningTotal);
        newlyReturnedBill.setHospitalFee(0 - returningHospitalTotal);
        newlyReturnedBill.setCollctingCentreFee(0 - returningCCTotal);
        newlyReturnedBill.setProfessionalFee(0 - returningStaffTotal);
        newlyReturnedBill.setDiscount(0 - returningDiscount);

        newlyReturnedBill.setTotalHospitalFee(0 - returningHospitalTotal);
        newlyReturnedBill.setTotalCenterFee(0 - returningCCTotal);
        newlyReturnedBill.setTotalStaffFee(0 - returningStaffTotal);

        billController.save(newlyReturnedBill);

        System.out.println("CC Balance Update ");
        //Update Centre Balanace
//        System.out.println("Institution = " + originalBillToReturn.getCollectingCentre());
//        System.out.println("Hospital Fee = " + newlyReturnedBill.getHospitalFee());
//        System.out.println("CollctingCentre Fee = " + newlyReturnedBill.getCollctingCentreFee());
//        System.out.println("Professional Fee = " + newlyReturnedBill.getProfessionalFee());
//        System.out.println("Net Total = " + newlyReturnedBill.getNetTotal());
//        System.out.println("History Type = " + HistoryType.CollectingCentreBillingRefund);
//        System.out.println("Bill = " + newlyReturnedBill);

        agentAndCcApplicationController.updateCcBalance(
                originalBillToReturn.getCollectingCentre(),
                newlyReturnedBill.getHospitalFee(),
                newlyReturnedBill.getCollctingCentreFee(),
                newlyReturnedBill.getProfessionalFee(),
                newlyReturnedBill.getNetTotal(),
                HistoryType.CollectingCentreBillingRefund,
                newlyReturnedBill);

        // drawer Update (No Need Update Drawer)
//      drawerController.updateDrawerForOuts(returningPayment);
        returningStarted = false;
        return "/collecting_centre/cc_bill_return_print?faces-redirect=true";

    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public Bill getOriginalBillToReturn() {
        return originalBillToReturn;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setOriginalBillToReturn(Bill originalBillToReturn) {
        this.originalBillToReturn = originalBillToReturn;
    }

    public List<BillItem> getOriginalBillItemsAvailableToReturn() {
        return originalBillItemsAvailableToReturn;
    }

    public void setOriginalBillItemsAvailableToReturn(List<BillItem> originalBillItemsAvailableToReturn) {
        this.originalBillItemsAvailableToReturn = originalBillItemsAvailableToReturn;
    }

    public List<BillItem> getOriginalBillItemsToSelectedToReturn() {
        return originalBillItemsToSelectedToReturn;
    }

    public void setOriginalBillItemsToSelectedToReturn(List<BillItem> originalBillItemsToSelectedToReturn) {
        this.originalBillItemsToSelectedToReturn = originalBillItemsToSelectedToReturn;
    }

    public Bill getNewlyReturnedBill() {
        return newlyReturnedBill;
    }

    public void setNewlyReturnedBill(Bill newlyReturnedBill) {
        this.newlyReturnedBill = newlyReturnedBill;
    }

    public List<BillItem> getNewlyReturnedBillItems() {
        return newlyReturnedBillItems;
    }

    public void setNewlyReturnedBillItems(List<BillItem> newlyReturnedBillItems) {
        this.newlyReturnedBillItems = newlyReturnedBillItems;
    }

    public List<BillFee> getNewlyReturnedBillFees() {
        return newlyReturnedBillFees;
    }

    public void setNewlyReturnedBillFees(List<BillFee> newlyReturnedBillFees) {
        this.newlyReturnedBillFees = newlyReturnedBillFees;
    }

    public List<Payment> getReturningBillPayments() {
        return returningBillPayments;
    }

    public void setReturningBillPayments(List<Payment> returningBillPayments) {
        this.returningBillPayments = returningBillPayments;
    }

    public boolean isReturningStarted() {
        return returningStarted;
    }

    public void setReturningStarted(boolean returningStarted) {
        this.returningStarted = returningStarted;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getRefundingTotalAmount() {
        return refundingTotalAmount;
    }

    public void setRefundingTotalAmount(double refundingTotalAmount) {
        this.refundingTotalAmount = refundingTotalAmount;
    }

    public String getRefundComment() {
        return refundComment;
    }

    public void setRefundComment(String refundComment) {
        this.refundComment = refundComment;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    // </editor-fold>
    @Override
    public double calculatRemainForMultiplePaymentTotal() {
        throw new UnsupportedOperationException("Multiple Payments Not supported in Returns and Refunds.");
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        throw new UnsupportedOperationException("Multiple Payments Not supported in Returns and Refunds");
    }

    @Override
    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        throw new UnsupportedOperationException("Multiple Payments Not supported in Returns and Refunds");
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }
}
