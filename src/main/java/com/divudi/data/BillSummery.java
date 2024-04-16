/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

import com.divudi.entity.WebUser;

/**
 *
 * @author buddhika_ari
 */
public class BillSummery {

    PaymentMethod paymentMethod;
    Double total;
    Double discount;
    Double netTotal;
    Double tax;
    Long count;
    BillType billType;
    WebUser webUser;
    private Long key;
    private BillClassType billClassType;

    public BillSummery() {
    }

    public BillSummery(PaymentMethod paymentMethod, Double total, Double discount, Double netTotal, Double tax, Long count, BillType billType) {
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
        this.tax = tax;
        this.count = count;
        this.billType = billType;
    }

    public BillSummery(PaymentMethod paymentMethod, BillClassType billClassType, Double total, Double discount, Double netTotal, Double tax, Long count, BillType billType) {
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
        this.tax = tax;
        this.count = count;
        this.billType = billType;
        this.billClassType = billClassType;
    }

    public BillSummery(PaymentMethod paymentMethod, BillClassType billClassType, Double total, Double discount, Double netTotal, Double tax, Long count, BillType billType, WebUser webUser) {
        this.paymentMethod = paymentMethod;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
        this.tax = tax;
        this.count = count;
        this.billType = billType;
        this.billClassType = billClassType;
        this.webUser = webUser;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public Long getKey() {
        return key;
    }

    public void setKey(Long key) {
        this.key = key;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

}
