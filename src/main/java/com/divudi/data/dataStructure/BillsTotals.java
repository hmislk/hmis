/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import java.util.List;

/**
 *
 * @author safrin
 */
public class BillsTotals {
    private String name;
    private List<Bill> bills;
    private List<BillItem> billItems;
    private double cash;
    private double credit;
    private double card;
    private double cheque;
    private double expense;
    private double grnNetTotalWithExpenses;
    private double slip;
    private double agent;
    double saleCash;
    double saleCredit;
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
            if (null != b.getPaymentMethod()) switch (b.getPaymentMethod()) {
                case Cash:
                    setCash(getCash() + b.getNetTotal());
                    break;
                case Credit:
                    setCredit(getCredit() + b.getNetTotal());
                    break;
                case Card:
                    setCard(getCard() + b.getNetTotal());
                    break;
                case Cheque:
                    setCheque(getCheque() + b.getNetTotal());
                    break;
                case Slip:
                    setSlip(getSlip() + b.getNetTotal());
                    break;
                case Agent:
                    setSlip(getAgent() + b.getNetTotal());
                    break;
                default:
                    break;
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

    public double getSaleCash() {
        return saleCash;
    }

    public void setSaleCash(double saleCash) {
        this.saleCash = saleCash;
    }

    public double getSaleCredit() {
        return saleCredit;
    }

    public void setSaleCredit(double saleCredit) {
        this.saleCredit = saleCredit;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

}
