/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.membership;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.PriceMatrix;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public class IpaPaymentMethod {

    PaymentMethod paymentMethod;
    List<PriceMatrix> priceMatrixs;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<PriceMatrix> getPriceMatrixs() {
        if (priceMatrixs == null) {
            priceMatrixs = new ArrayList<>();
        }
        return priceMatrixs;
    }

    public void setPriceMatrixs(List<PriceMatrix> priceMatrixs) {
        this.priceMatrixs = priceMatrixs;
    }

}
