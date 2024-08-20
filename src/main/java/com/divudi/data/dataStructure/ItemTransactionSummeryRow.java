/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Item;
import java.util.Date;

/**
 *
 * @author buddhika
 */
public class ItemTransactionSummeryRow implements Comparable<ItemTransactionSummeryRow> {

    Item item;
    double retailSaleQty;
    double wholeSaleQty;
    double bhtSaleQty;
    double transferOutQty;
    double issueQty;
    double totalOutQty;
    double transferInQty;
    double purchaseQty;
    double totalInQty;

    double retailSaleVal;
    double wholeSaleVal;
    double issueVal;
    double bhtSaleVal;
    double transferOutVal;
    double totalOutVal;
    double transferInVal;
    double purchaseVal;
    double totalInVal;

    double value;
    Date date;
    double quantity;

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public double getIssueQty() {
        return issueQty;
    }

    public void setIssueQty(double issueQty) {
        this.issueQty = issueQty;
    }

    public double getIssueVal() {
        return issueVal;
    }

    public void setIssueVal(double issueVal) {
        this.issueVal = issueVal;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getRetailSaleQty() {
        return retailSaleQty;
    }

    public void setRetailSaleQty(double retailSaleQty) {
        this.retailSaleQty = retailSaleQty;
    }

    public double getWholeSaleQty() {
        return wholeSaleQty;
    }

    public void setWholeSaleQty(double wholeSaleQty) {
        this.wholeSaleQty = wholeSaleQty;
    }

    public double getBhtSaleQty() {
        return bhtSaleQty;
    }

    public void setBhtSaleQty(double bhtSaleQty) {
        this.bhtSaleQty = bhtSaleQty;
    }

    public double getTransferOutQty() {
        return transferOutQty;
    }

    public void setTransferOutQty(double transferOutQty) {
        this.transferOutQty = transferOutQty;
    }

    public double getTotalOutQty() {
        return totalOutQty;
    }

    public void setTotalOutQty(double totalOutQty) {
        this.totalOutQty = totalOutQty;
    }

    public double getTransferInQty() {
        return transferInQty;
    }

    public void setTransferInQty(double transferInQty) {
        this.transferInQty = transferInQty;
    }

    public double getPurchaseQty() {
        return purchaseQty;
    }

    public void setPurchaseQty(double purchaseQty) {
        this.purchaseQty = purchaseQty;
    }

    public double getTotalInQty() {
        return totalInQty;
    }

    public void setTotalInQty(double totalInQty) {
        this.totalInQty = totalInQty;
    }

    public double getRetailSaleVal() {
        return retailSaleVal;
    }

    public void setRetailSaleVal(double retailSaleVal) {
        this.retailSaleVal = retailSaleVal;
    }

    public double getWholeSaleVal() {
        return wholeSaleVal;
    }

    public void setWholeSaleVal(double wholeSaleVal) {
        this.wholeSaleVal = wholeSaleVal;
    }

    public double getBhtSaleVal() {
        return bhtSaleVal;
    }

    public void setBhtSaleVal(double bhtSaleVal) {
        this.bhtSaleVal = bhtSaleVal;
    }

    public double getTransferOutVal() {
        return transferOutVal;
    }

    public void setTransferOutVal(double transferOutVal) {
        this.transferOutVal = transferOutVal;
    }

    public double getTotalOutVal() {
        return totalOutVal;
    }

    public void setTotalOutVal(double totalOutVal) {
        this.totalOutVal = totalOutVal;
    }

    public double getTransferInVal() {
        return transferInVal;
    }

    public void setTransferInVal(double transferInVal) {
        this.transferInVal = transferInVal;
    }

    public double getPurchaseVal() {
        return purchaseVal;
    }

    public void setPurchaseVal(double purchaseVal) {
        this.purchaseVal = purchaseVal;
    }

    public double getTotalInVal() {
        return totalInVal;
    }

    public void setTotalInVal(double totalInVal) {
        this.totalInVal = totalInVal;
    }

    @Override
    public int compareTo(ItemTransactionSummeryRow t) {
        if (item == null) {
            return 0;
        }
        if (t == null || t.item == null) {
            return 1;
        }
        return item.getName().compareTo(t.getItem().getName());
    }

}
