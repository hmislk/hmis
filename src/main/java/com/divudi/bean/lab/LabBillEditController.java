/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.UtilityController;
import com.divudi.entity.Bill;
import com.divudi.facade.BillFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class LabBillEditController implements Serializable {

    /**
     * Creates a new instance of LabBillEditController
     */
    public LabBillEditController() {
    }

    Bill bill;
    @EJB
    BillFacade billFacade;

    public void updateBill() {
        if (bill == null) {
            UtilityController.addErrorMessage("Select a bill");
            return;
        }
        getBillFacade().edit(bill);
        UtilityController.addErrorMessage("Updates");
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

}
