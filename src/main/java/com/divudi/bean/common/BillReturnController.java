package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.PaymentController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.facade.BillFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Damith
 *
 */
@Named
@SessionScoped
public class BillReturnController implements Serializable {

    @EJB
    BillFacade billFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;

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
    PaymentController paymentController;
    @Inject
    DrawerController drawerController;

    private Bill originalBillToReturn;
    private List<BillItem> originalBillItemsAvailableToReturn;
    private List<BillItem> originalBillItemsToSelectedToReturn;

    private Bill newlyReturnedBill;
    private List<BillItem> newlyReturnedBillItems;
    private List<BillFee> newlyReturnedBillFees;
    private List<Payment> returningBillPayments;

    private PaymentMethod paymentMethod;
  
    private boolean returningStarted = false;
    private double refundingTotalAmount;
    private String refundComment;
    private boolean selectAll;

    /**
     * Creates a new instance of BillReturnController
     */
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

    public String navigateToReturnOpdBill() {
        if (originalBillToReturn == null) {
            return null;
        }
        
        originalBillItemsAvailableToReturn = billBeanController.fetchBillItems(originalBillToReturn);
        returningStarted = false;
        paymentMethod = originalBillToReturn.getPaymentMethod();
        return "/opd/bill_return?faces-redirect=true";
    }
    
    public String navigateToOPDBillSearchFormRefundOpdBillView() {
        return"/opd/opd_bill_search?faces-redirect=true";
    }

    public void selectAllItems() {
        originalBillItemsToSelectedToReturn = new ArrayList();
        for (BillItem selectedBillItemToReturn : originalBillItemsAvailableToReturn) {
            if (!selectedBillItemToReturn.isRefunded()) {
                System.out.println(selectedBillItemToReturn.getItem().getName() + " Add");
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
    
    public boolean checkCanReturnBill(Bill bill){
        List<BillItem> items = billBeanController.fetchBillItems(bill);
        boolean canReturn = false;
        for(BillItem bllItem : items){
            if(!bllItem.isRefunded()){
                canReturn = true;
            }
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
        newlyReturnedBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_REFUND);
        newlyReturnedBill.setComments(refundComment);
        newlyReturnedBill.setInstitution(sessionController.getInstitution());
        newlyReturnedBill.setDepartment(sessionController.getDepartment());
        newlyReturnedBill.invertValue();

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
        double returningDiscount = 0.0;

        newlyReturnedBillItems = new ArrayList<>();
        returningBillPayments = new ArrayList<>();
        newlyReturnedBillFees = new ArrayList<>();
      
        for (BillItem selectedBillItemToReturn : originalBillItemsToSelectedToReturn) {

            returningTotal += selectedBillItemToReturn.getGrossValue();
            returningNetTotal += selectedBillItemToReturn.getNetValue();
            returningHospitalTotal += selectedBillItemToReturn.getHospitalFee();
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
        newlyReturnedBill.setProfessionalFee(0 - returningStaffTotal);
        newlyReturnedBill.setDiscount(0 - returningDiscount);
        billController.save(newlyReturnedBill);

        Payment returningPayment = new Payment();
        returningPayment.setBill(newlyReturnedBill);
        returningPayment.setPaymentMethod(paymentMethod);
        returningPayment.setPaidValue(newlyReturnedBill.getNetTotal());
        paymentController.save(returningPayment);
        returningBillPayments.add(returningPayment);

//      drawer Update
        drawerController.updateDrawerForOuts(returningPayment);
        
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
        }else{
            selectAll = false;
        }
    }

    public Bill getOriginalBillToReturn() {
        return originalBillToReturn;
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

}
