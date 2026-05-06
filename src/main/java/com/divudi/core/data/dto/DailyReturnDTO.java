package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;

/**
 * Lightweight projection for Daily Return summaries.
 */
public class DailyReturnDTO implements Serializable {
    private PaymentMethod paymentMethod;
    private Double total;

    public DailyReturnDTO() {
    }

    public DailyReturnDTO(PaymentMethod paymentMethod, Double total) {
        this.paymentMethod = paymentMethod;
        this.total = total;
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
}
