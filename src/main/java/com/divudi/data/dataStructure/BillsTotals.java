/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.data.PaymentMethod;
import com.divudi.entity.Bill;
import java.util.List;

/**
 *
 * @author safrin
 */
public class BillsTotals {
    private String name;
    private List<Bill> bills;
    private double cash;
    private double credit;
    private double card;
    private double cheque;
    private double expense;
    private double grnNetTotalWithExpenses;
    private double slip;
    private double agent;
    private boolean bold;
    //private BillType billType;

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
       // calTot();
    }

    private void calTot() {
        cash = credit = card = cheque = slip = agent = 0.0;
        for (Bill b : bills) {
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                setCash(getCash() + b.getNetTotal());
            } else if (b.getPaymentMethod() == PaymentMethod.Credit) {
                setCredit(getCredit() + b.getNetTotal());
            } else if (b.getPaymentMethod() == PaymentMethod.Card) {
                setCard(getCard() + b.getNetTotal());
            } else if (b.getPaymentMethod() == PaymentMethod.Cheque) {
                setCheque(getCheque() + b.getNetTotal());
            } else if (b.getPaymentMethod() == PaymentMethod.Slip) {
                setSlip(getSlip() + b.getNetTotal());
            } else if (b.getPaymentMethod() == PaymentMethod.Agent) {
                setSlip(getAgent() + b.getNetTotal());
            }

        }

    }
    
//    private double getTotal(BillType billType){
//        String sql="";
//        HashMap hm=new HashMap();
//        hm.put("btp", billType);
//    
//    }

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public double getCredit() {
        return credit;
    }

    public void setCredit(double credit) {
        this.credit = credit;
    }

    public double getCard() {
        return card;
    }

    public void setCard(double card) {
        this.card = card;
    }

    public double getCheque() {
        return cheque;
    }

    public void setCheque(double cheque) {
        this.cheque = cheque;
    }

    public double getSlip() {
        return slip;
    }

    public void setSlip(double slip) {
        this.slip = slip;
    }
    
    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isBold() {
        return bold;
    }
    
    public boolean getBold(){
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public double getExpense() {
        return expense;
    }

    public void setExpense(double expense) {
        this.expense = expense;
    }

    public double getGrnNetTotalWithExpenses() {
        return grnNetTotalWithExpenses;
    }

    public void setGrnNetTotalWithExpenses(double grnNetTotalWithExpenses) {
        this.grnNetTotalWithExpenses = grnNetTotalWithExpenses;
    }

    public double getAgent() {
        return agent;
    }

    public void setAgent(double agent) {
        this.agent = agent;
    }

   
   

}
