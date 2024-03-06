/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.FeeType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.PriceMatrix;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.RowEditEvent;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ServiceFeeEdit implements Serializable {

    private BillItem billItem;
    private List<BillFee> billFees;
    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    private InwardBeanController inwardBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFacade billFacade;
    @Inject
    private SessionController sessionController;
    double billNetTotal;
    double billItemNetTotal;

    public double getBillNetTotal() {
        return billNetTotal;
    }

    public void setBillNetTotal(double billNetTotal) {
        this.billNetTotal = billNetTotal;
    }

    public double getBillItemNetTotal() {
        return billItemNetTotal;
    }

    public void setBillItemNetTotal(double billItemNetTotal) {
        this.billItemNetTotal = billItemNetTotal;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    /**
     * Creates a new instance of ServiceFeeEdit
     */
    public ServiceFeeEdit() {
    }

    private void calBillFees() {
        //      System.err.println("Calculating BillFee 1 " + billItem);
        String sql = "SELECT b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.billItem=:billItem ";
        HashMap hm = new HashMap();
        hm.put("billItem", billItem);
        billFees = getBillFeeFacade().findByJpql(sql, hm);

        // System.err.println("Calculating BillFee 2 " + billFees);
    }

    @Inject
    BillBeanController billBean;

    @Inject
    PriceMatrixController priceMatrixController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public void updateFee(RowEditEvent event) {
        BillFee billFee = (BillFee) event.getObject();

        if (billFee == null) {
            return;
        }

        if (billFee.getFee() == null) {
            return;
        }

        if (billFee.getFee().getFeeType() == FeeType.Staff) {
            if (billFee.getPaidValue() != 0) {
                JsfUtil.addErrorMessage("Staff Fee Allready Paid");
                return;
            }
        }

        if (billFee.getBill() == null) {
            return;
        }

        if (billFee.getBill().getCheckedBy() != null) {
            JsfUtil.addErrorMessage("This Bill Already Checked");
            return;
        }

        billFee.setEditor(getSessionController().getLoggedUser());
        billFee.setEditedAt(new Date());

        billItem.setEditor(getSessionController().getLoggedUser());
        billItem.setEditedAt(new Date());

        billItem.getBill().setEditor(getSessionController().getLoggedUser());
        billItem.getBill().setEditedAt(new Date());

        getBillFeeFacade().edit(billFee);

        PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(billItem, billFee.getFeeGrossValue(), billItem.getBill().getFromDepartment(),billItem.getBill().getPatientEncounter().getPaymentMethod());

        getInwardBean().updateBillItemMargin(billItem, billFee.getFeeGrossValue(), billItem.getBill().getPatientEncounter(), billItem.getBill().getFromDepartment(), priceMatrix);

        getBillBean().updateBillItemByBillFee(billItem);
        getBillBean().updateBillByBillFee(billItem.getBill());

        calBillFees();

        billItem = getBillItemFacade().find(billFee.getBillItem().getId());

        Bill b = getBillFacade().find(billFee.getBill().getId());
        billItem.setBill(b);

        billItemNetTotal = billItem.getNetValue();
        billNetTotal = billItem.getBill().getNetTotal();
    }

    public List<BillFee> getBillFees() {
        if (billFees == null) {
            billFees = new ArrayList<>();
        }
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;

    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;

        billItemNetTotal = billItem.getNetValue();
        billNetTotal = billItem.getBill().getNetTotal();
        calBillFees();
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

}
