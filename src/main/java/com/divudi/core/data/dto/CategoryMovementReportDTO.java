package com.divudi.core.data.dto;

import java.io.Serializable;

public class CategoryMovementReportDTO implements Serializable {

    private Long itemId;
    private String itemName;
    private String itemCode;
    private Double opdSale;
    private Double opdSaleQty;
    private Double inwardIssue;
    private Double inwardIssueQty;
    private Double departmentIssue;
    private Double departmentIssueQty;
    private Double total;
    private Double totalQty;
    private Double purchaseValue;
    private Double marginValue;
    private Double transferIn;
    private Double transferInQty;
    private Double transferOut;
    private Double transferOutQty;
    // Issue #18635: kind of row (Movement, or Cancellation / Return) and the new movement columns
    private String rowType;
    private Double wholesale;
    private Double wholesaleQty;
    private Double disposal;
    private Double disposalQty;
    private Double transferReceive;
    private Double transferReceiveQty;

    public CategoryMovementReportDTO() {
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Double getOpdSale() {
        return opdSale != null ? opdSale : 0.0;
    }

    public void setOpdSale(Double opdSale) {
        this.opdSale = opdSale;
    }

    public Double getOpdSaleQty() {
        return opdSaleQty != null ? opdSaleQty : 0.0;
    }

    public void setOpdSaleQty(Double opdSaleQty) {
        this.opdSaleQty = opdSaleQty;
    }

    public Double getInwardIssue() {
        return inwardIssue != null ? inwardIssue : 0.0;
    }

    public void setInwardIssue(Double inwardIssue) {
        this.inwardIssue = inwardIssue;
    }

    public Double getInwardIssueQty() {
        return inwardIssueQty != null ? inwardIssueQty : 0.0;
    }

    public void setInwardIssueQty(Double inwardIssueQty) {
        this.inwardIssueQty = inwardIssueQty;
    }

    public Double getDepartmentIssue() {
        return departmentIssue != null ? departmentIssue : 0.0;
    }

    public void setDepartmentIssue(Double departmentIssue) {
        this.departmentIssue = departmentIssue;
    }

    public Double getDepartmentIssueQty() {
        return departmentIssueQty != null ? departmentIssueQty : 0.0;
    }

    public void setDepartmentIssueQty(Double departmentIssueQty) {
        this.departmentIssueQty = departmentIssueQty;
    }

    public Double getTotal() {
        return total != null ? total : 0.0;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getTotalQty() {
        return totalQty != null ? totalQty : 0.0;
    }

    public void setTotalQty(Double totalQty) {
        this.totalQty = totalQty;
    }

    public Double getPurchaseValue() {
        return purchaseValue != null ? purchaseValue : 0.0;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getMarginValue() {
        return marginValue != null ? marginValue : 0.0;
    }

    public void setMarginValue(Double marginValue) {
        this.marginValue = marginValue;
    }

    public Double getTransferIn() {
        return transferIn != null ? transferIn : 0.0;
    }

    public void setTransferIn(Double transferIn) {
        this.transferIn = transferIn;
    }

    public Double getTransferInQty() {
        return transferInQty != null ? transferInQty : 0.0;
    }

    public void setTransferInQty(Double transferInQty) {
        this.transferInQty = transferInQty;
    }

    public Double getTransferOut() {
        return transferOut != null ? transferOut : 0.0;
    }

    public void setTransferOut(Double transferOut) {
        this.transferOut = transferOut;
    }

    public Double getTransferOutQty() {
        return transferOutQty != null ? transferOutQty : 0.0;
    }

    public void setTransferOutQty(Double transferOutQty) {
        this.transferOutQty = transferOutQty;
    }

    public String getRowType() {
        return rowType;
    }

    public void setRowType(String rowType) {
        this.rowType = rowType;
    }

    public Double getWholesale() {
        return wholesale != null ? wholesale : 0.0;
    }

    public void setWholesale(Double wholesale) {
        this.wholesale = wholesale;
    }

    public Double getWholesaleQty() {
        return wholesaleQty != null ? wholesaleQty : 0.0;
    }

    public void setWholesaleQty(Double wholesaleQty) {
        this.wholesaleQty = wholesaleQty;
    }

    public Double getDisposal() {
        return disposal != null ? disposal : 0.0;
    }

    public void setDisposal(Double disposal) {
        this.disposal = disposal;
    }

    public Double getDisposalQty() {
        return disposalQty != null ? disposalQty : 0.0;
    }

    public void setDisposalQty(Double disposalQty) {
        this.disposalQty = disposalQty;
    }

    public Double getTransferReceive() {
        return transferReceive != null ? transferReceive : 0.0;
    }

    public void setTransferReceive(Double transferReceive) {
        this.transferReceive = transferReceive;
    }

    public Double getTransferReceiveQty() {
        return transferReceiveQty != null ? transferReceiveQty : 0.0;
    }

    public void setTransferReceiveQty(Double transferReceiveQty) {
        this.transferReceiveQty = transferReceiveQty;
    }
}
