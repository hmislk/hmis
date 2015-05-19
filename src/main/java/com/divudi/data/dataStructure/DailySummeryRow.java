/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import java.util.Date;

/**
 *
 * @author buddhika
 */
public class DailySummeryRow {
    Date summeryDate;
    Double hospitalFeeTotal;
    Double staffFeeTotal;
    Double reagentFeeTotal;
    Double totalFee;
    Long billFeeCount;
    Long billCount;
    Long billItemCount;

    public DailySummeryRow(Date summeryDate, Double hospitalFeeTotal, Double staffFeeTotal, Double reagentFeeTotal, Double totalFee, Long billFeeCount, Long billCount, Long billItemCount) {
        this.summeryDate = summeryDate;
        this.hospitalFeeTotal = hospitalFeeTotal;
        this.staffFeeTotal = staffFeeTotal;
        this.reagentFeeTotal = reagentFeeTotal;
        this.totalFee = totalFee;
        this.billFeeCount = billFeeCount;
        this.billCount = billCount;
        this.billItemCount = billItemCount;
    }

    
    
    public DailySummeryRow() {
    }

    
    
    public Date getSummeryDate() {
        return summeryDate;
    }

    public void setSummeryDate(Date summeryDate) {
        this.summeryDate = summeryDate;
    }

    public Double getHospitalFeeTotal() {
        return hospitalFeeTotal;
    }

    public void setHospitalFeeTotal(Double hospitalFeeTotal) {
        this.hospitalFeeTotal = hospitalFeeTotal;
    }

    public Double getStaffFeeTotal() {
        return staffFeeTotal;
    }

    public void setStaffFeeTotal(Double staffFeeTotal) {
        this.staffFeeTotal = staffFeeTotal;
    }

    public Double getReagentFeeTotal() {
        return reagentFeeTotal;
    }

    public void setReagentFeeTotal(Double reagentFeeTotal) {
        this.reagentFeeTotal = reagentFeeTotal;
    }

    public Double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(Double totalFee) {
        this.totalFee = totalFee;
    }

    public Long getBillFeeCount() {
        return billFeeCount;
    }

    public void setBillFeeCount(Long billFeeCount) {
        this.billFeeCount = billFeeCount;
    }

    public Long getBillCount() {
        return billCount;
    }

    public void setBillCount(Long billCount) {
        this.billCount = billCount;
    }

    public Long getBillItemCount() {
        return billItemCount;
    }

    public void setBillItemCount(Long billItemCount) {
        this.billItemCount = billItemCount;
    }
    
    
    
    
    
}
