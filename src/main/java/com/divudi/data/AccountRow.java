/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;

/**
 *
 * @author buddhika
 */
public class AccountRow {

    Bill bill;
    BillItem billItem;
    PaymentMethod paymentMethod;
    BillType billType;
    double value;
    double doctorFee;
    double hospitalFee;
    double scanFee;
    double tax;
    double agencyFee;
    String name;
    String comments;
    int intNo;
    double cashTotal;
    double agentTotal;
    double chequeTotal;
    double slipTotal;
    double creditTotal;
    double cardTotal;
    double collectionTotal;

    public AccountRow() {
    }

    public double getCollectionTotal() {
        return collectionTotal;
    }

    public void setCollectionTotal(double collectionTotal) {
        this.collectionTotal = collectionTotal;
    }

    public double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public double getAgentTotal() {
        return agentTotal;
    }

    public void setAgentTotal(double agentTotal) {
        this.agentTotal = agentTotal;
    }

    public double getChequeTotal() {
        return chequeTotal;
    }

    public void setChequeTotal(double chequeTotal) {
        this.chequeTotal = chequeTotal;
    }

    public double getSlipTotal() {
        return slipTotal;
    }

    public void setSlipTotal(double slipTotal) {
        this.slipTotal = slipTotal;
    }

    public double getCreditTotal() {
        return creditTotal;
    }

    public void setCreditTotal(double creditTotal) {
        this.creditTotal = creditTotal;
    }

    public double getCardTotal() {
        return cardTotal;
    }

    public void setCardTotal(double cardTotal) {
        this.cardTotal = cardTotal;
    }

    public int getIntNo() {
        return intNo;
    }

    public void setIntNo(int intNo) {
        this.intNo = intNo;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(double doctorFee) {
        this.doctorFee = doctorFee;
    }

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getScanFee() {
        return scanFee;
    }

    public void setScanFee(double scanFee) {
        this.scanFee = scanFee;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getAgencyFee() {
        return agencyFee;
    }

    public void setAgencyFee(double agencyFee) {
        this.agencyFee = agencyFee;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

}
