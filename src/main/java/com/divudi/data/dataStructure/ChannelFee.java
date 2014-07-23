/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.BillFee;

/**
 *
 * @author safrin
 */
public class ChannelFee {

    private BillFee billedFee;
    private BillFee prevFee;
    private BillFee repayment;

    public BillFee getBilledFee() {
        if (billedFee == null) {
            billedFee = new BillFee();
        }
        return billedFee;
    }

    public void setBilledFee(BillFee billedFee) {
        this.billedFee = billedFee;
    }

    public BillFee getPrevFee() {
        if (prevFee == null) {
            prevFee = new BillFee();
        }
        return prevFee;
    }

    public void setPrevFee(BillFee prevFee) {
        this.prevFee = prevFee;
    }

    public BillFee getRepayment() {
        if (repayment == null) {
            repayment = new BillFee();
        }
        return repayment;
    }

    public void setRepayment(BillFee repayment) {
        this.repayment = repayment;
    }
}
