/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Item;
import java.util.Date;

/**
 *
 * @author buddhika
 */
public class ItemTransactionSummeryRow {
    Item item;
    Double retailSaleQty;
    Double wholeSaleQty;
    Double bhtSaleQty;
    Double transferOutQty;
    Double issueQty;
    Double totalOutQty;
    Double transferInQty;
    Double purchaseQty;
    Double totalInQty;
    
    Double retailSaleVal;
    Double wholeSaleVal;
    Double issueVal;
    Double bhtSaleVal;
    Double transferOutVal;
    Double totalOutVal;
    Double transferInVal;
    Double purchaseVal;
    Double totalInVal;
    
    Double value;
    Date date;
    Double quantity;

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
    
    

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    
    

    public Double getIssueQty() {
        return issueQty;
    }

    public void setIssueQty(Double issueQty) {
        this.issueQty = issueQty;
    }

    public Double getIssueVal() {
        return issueVal;
    }

    public void setIssueVal(Double issueVal) {
        this.issueVal = issueVal;
    }

    
    
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Double getRetailSaleQty() {
        return retailSaleQty;
    }

    public void setRetailSaleQty(Double retailSaleQty) {
        this.retailSaleQty = retailSaleQty;
    }

    public Double getWholeSaleQty() {
        return wholeSaleQty;
    }

    public void setWholeSaleQty(Double wholeSaleQty) {
        this.wholeSaleQty = wholeSaleQty;
    }

    public Double getBhtSaleQty() {
        return bhtSaleQty;
    }

    public void setBhtSaleQty(Double bhtSaleQty) {
        this.bhtSaleQty = bhtSaleQty;
    }

    public Double getTransferOutQty() {
        return transferOutQty;
    }

    public void setTransferOutQty(Double transferOutQty) {
        this.transferOutQty = transferOutQty;
    }

    public Double getTotalOutQty() {
        return totalOutQty;
    }

    public void setTotalOutQty(Double totalOutQty) {
        this.totalOutQty = totalOutQty;
    }

    public Double getTransferInQty() {
        return transferInQty;
    }

    public void setTransferInQty(Double transferInQty) {
        this.transferInQty = transferInQty;
    }

    public Double getPurchaseQty() {
        return purchaseQty;
    }

    public void setPurchaseQty(Double purchaseQty) {
        this.purchaseQty = purchaseQty;
    }

    public Double getTotalInQty() {
        return totalInQty;
    }

    public void setTotalInQty(Double totalInQty) {
        this.totalInQty = totalInQty;
    }

    public Double getRetailSaleVal() {
        return retailSaleVal;
    }

    public void setRetailSaleVal(Double retailSaleVal) {
        this.retailSaleVal = retailSaleVal;
    }

    public Double getWholeSaleVal() {
        return wholeSaleVal;
    }

    public void setWholeSaleVal(Double wholeSaleVal) {
        this.wholeSaleVal = wholeSaleVal;
    }

    public Double getBhtSaleVal() {
        return bhtSaleVal;
    }

    public void setBhtSaleVal(Double bhtSaleVal) {
        this.bhtSaleVal = bhtSaleVal;
    }

    public Double getTransferOutVal() {
        return transferOutVal;
    }

    public void setTransferOutVal(Double transferOutVal) {
        this.transferOutVal = transferOutVal;
    }

    public Double getTotalOutVal() {
        return totalOutVal;
    }

    public void setTotalOutVal(Double totalOutVal) {
        this.totalOutVal = totalOutVal;
    }

    public Double getTransferInVal() {
        return transferInVal;
    }

    public void setTransferInVal(Double transferInVal) {
        this.transferInVal = transferInVal;
    }

    public Double getPurchaseVal() {
        return purchaseVal;
    }

    public void setPurchaseVal(Double purchaseVal) {
        this.purchaseVal = purchaseVal;
    }

    public Double getTotalInVal() {
        return totalInVal;
    }

    public void setTotalInVal(Double totalInVal) {
        this.totalInVal = totalInVal;
    }

    
}
