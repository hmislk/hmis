/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class IntrimPrintController implements Serializable {

    Bill currentBill;
    Bill previousBill;
    @Inject
    BhtSummeryController bhtSummeryController;
    @Inject
    SessionController sessionController;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void calAdjustedTotal() {
        double dbl = 0;
        for (BillItem b : getCurrentBill().getBillItems()) {
            dbl += b.getAdjustedValue();
        }

        getCurrentBill().setAdjustedTotal(dbl);
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public void makeNull() {
        currentBill = null;
        previousBill = null;
    }

    public BhtSummeryController getBhtSummeryController() {
        return bhtSummeryController;
    }

    public void setBhtSummeryController(BhtSummeryController bhtSummeryController) {
        this.bhtSummeryController = bhtSummeryController;
    }

    public void saveIntrimBill() {
        double different = Math.abs((getCurrentBill().getTotal() - getCurrentBill().getAdjustedTotal()));

        if (different > 0.1) {
            JsfUtil.addErrorMessage("Please Adjust category amount correctly");
            return;
        }

        List<BillItem> list = getCurrentBill().getBillItems();
        getCurrentBill().setBillItems(null);

        getCurrentBill().setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.InwardIntrimBill, BillClassType.BilledBill, BillNumberSuffix.INWINTRIM));
        getCurrentBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.InwardIntrimBill, BillClassType.BilledBill, BillNumberSuffix.INWINTRIM));
        getCurrentBill().setDepartment(getSessionController().getDepartment());
        getCurrentBill().setInstitution(getSessionController().getInstitution());
        getCurrentBill().setCreatedAt(new Date());
        getCurrentBill().setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(getCurrentBill());

        for (BillItem b : list) {
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());
            getBillItemFacade().create(b);
        }

        getCurrentBill().setBillItems(list);
        getBillFacade().edit(getCurrentBill());

    }

    public Bill getCurrentBill() {
        if (currentBill == null) {
            currentBill = new BilledBill();
            currentBill.setBillType(BillType.InwardIntrimBill);
        }
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public Bill getPreviousBill() {
        return previousBill;
    }

    public void setPreviousBill(Bill previousBill) {
        this.previousBill = previousBill;
    }

    /**
     * Creates a new instance of IntrimPrintController
     */
    public IntrimPrintController() {
    }

}
