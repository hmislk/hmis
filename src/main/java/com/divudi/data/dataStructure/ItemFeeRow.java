/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.data.dataStructure;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class ItemFeeRow {
    private Long hospitalFeeId;
    private Long collectingCentreFeeId;
    private String itemName;
    private String itemInstitution;
    private String feeInstitution;
    private String itemDepartment;
    private Double hospitalFee;
    private Double collectingCentreFee;
    private Double hospitalFeeForForeigners;
    private Double collectingCentreFeeForForeigners;
    private Long itemId;

    public ItemFeeRow() {
    }
    
    

    public Long getHospitalFeeId() {
        return hospitalFeeId;
    }

    public void setHospitalFeeId(Long hospitalFeeId) {
        this.hospitalFeeId = hospitalFeeId;
    }

    public Long getCollectingCentreFeeId() {
        return collectingCentreFeeId;
    }

    public void setCollectingCentreFeeId(Long collectingCentreFeeId) {
        this.collectingCentreFeeId = collectingCentreFeeId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemInstitution() {
        return itemInstitution;
    }

    public void setItemInstitution(String itemInstitution) {
        this.itemInstitution = itemInstitution;
    }

    public String getItemDepartment() {
        return itemDepartment;
    }

    public void setItemDepartment(String itemDepartment) {
        this.itemDepartment = itemDepartment;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getCollectingCentreFee() {
        return collectingCentreFee;
    }

    public void setCollectingCentreFee(Double collectingCentreFee) {
        this.collectingCentreFee = collectingCentreFee;
    }

    public Double getHospitalFeeForForeigners() {
        return hospitalFeeForForeigners;
    }

    public void setHospitalFeeForForeigners(Double hospitalFeeForForeigners) {
        this.hospitalFeeForForeigners = hospitalFeeForForeigners;
    }

    public Double getCollectingCentreFeeForForeigners() {
        return collectingCentreFeeForForeigners;
    }

    public void setCollectingCentreFeeForForeigners(Double collectingCentreFeeForForeigners) {
        this.collectingCentreFeeForForeigners = collectingCentreFeeForForeigners;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getFeeInstitution() {
        return feeInstitution;
    }

    public void setFeeInstitution(String feeInstitution) {
        this.feeInstitution = feeInstitution;
    }
    
    
    
    
}
