package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
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
    BillController billController;
    @Inject
    BillBeanController billBeanController;

    private Bill originalBillToReturn;
    private List<BillItem> originalBillItemsAvailableToReturn;
    private List<BillItem> originalBillItemsToSelectedToReturn;

    private Bill newlyReturnedBill;
    private List<BillItem> newlyReturnedBillItems;
    private List<BillFee> newlyReturnedBillFees;
    private List<Payment> returningBillPayment;

    private boolean returningStarted = false;

    /**
     * Creates a new instance of BillReturnController
     */
    public BillReturnController() {
    }

    public String navigateToReturnOpdBill() {
        if (originalBillToReturn == null) {
            return null;
        }
        originalBillItemsAvailableToReturn = billBeanController.fetchBillItems(originalBillToReturn);
        return "/opd/bill_return?faces-redirect=true";
    }

    public void settleOpdReturnBill() {
        if (returningStarted) {
            JsfUtil.addErrorMessage("Already Returning Started");
            return;
        }
        if (originalBillToReturn == null) {
            JsfUtil.addErrorMessage("Already Returning Started");
            return;
        }
        if(originalBillItemsToSelectedToReturn==null || originalBillItemsToSelectedToReturn.isEmpty()){
            JsfUtil.addErrorMessage("Nothing selected to return");
            return;
        }
        originalBillToReturn = billFacade.findWithoutCache(originalBillToReturn.getId());
        if(originalBillToReturn.isCancelled()){
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }
        if(originalBillToReturn.isRefunded()){
            JsfUtil.addErrorMessage("Already Cancelled");
            return;
        }
        // fetch original bill now, checked alteady returned, cancelled, drawer balance, 
        newlyReturnedBill = new RefundBill();
        newlyReturnedBill.copy(originalBillToReturn);
        newlyReturnedBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_REFUND);
        newlyReturnedBill.invertValue();
        billController.save(newlyReturnedBill);
        
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

    public List<Payment> getReturningBillPayment() {
        return returningBillPayment;
    }

    public void setReturningBillPayment(List<Payment> returningBillPayment) {
        this.returningBillPayment = returningBillPayment;
    }

    public boolean isReturningStarted() {
        return returningStarted;
    }

    public void setReturningStarted(boolean returningStarted) {
        this.returningStarted = returningStarted;
    }
    
    

}
