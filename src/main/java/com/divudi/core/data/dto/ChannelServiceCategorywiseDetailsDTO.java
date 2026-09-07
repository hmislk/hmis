
package com.divudi.core.data.dto;

import com.divudi.core.entity.Category;

/**
 *
 * @author CHINTHAKA
 */
public class ChannelServiceCategorywiseDetailsDTO {
    
    private String category;
    private Double cashTotal;
    private Double cardTotal;
    private Double agentTotal;
    private Double cancelTotal;
    private Double refundTotal;
    private Double totalHospitalPayment;
    private Double totalDoctorPayment;

    public ChannelServiceCategorywiseDetailsDTO(String category, Double cashTotal, Double cardTotal, Double agentTotal, Double cancelTotal, Double refundTotal, Double totalHospitalPayment, Double totalDoctorPayment) {
       
        this.category = category;
        this.cashTotal = cashTotal;
        this.cardTotal = cardTotal;
        this.agentTotal = agentTotal;
        this.cancelTotal = cancelTotal;
        this.refundTotal = refundTotal;
        this.totalHospitalPayment = totalHospitalPayment;
        this.totalDoctorPayment = totalDoctorPayment;
    }

    public ChannelServiceCategorywiseDetailsDTO() {
    }
    

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(Double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public Double getCardTotal() {
        return cardTotal;
    }

    public void setCardTotal(Double cardTotal) {
        this.cardTotal = cardTotal;
    }

    public Double getAgentTotal() {
        return agentTotal;
    }

    public void setAgentTotal(Double agentTotal) {
        this.agentTotal = agentTotal;
    }

    public Double getCancelTotal() {
        return cancelTotal;
    }

    public void setCancelTotal(Double cancelTotal) {
        this.cancelTotal = cancelTotal;
    }

    public Double getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(Double refundTotal) {
        this.refundTotal = refundTotal;
    }

    public Double getTotalHospitalPayment() {
        return totalHospitalPayment;
    }

    public void setTotalHospitalPayment(Double totalHospitalPayment) {
        this.totalHospitalPayment = totalHospitalPayment;
    }

    public Double getTotalDoctorPayment() {
        return totalDoctorPayment;
    }

    public void setTotalDoctorPayment(Double totalDoctorPayment) {
        this.totalDoctorPayment = totalDoctorPayment;
    }
    
    
    
    
    
    
}
