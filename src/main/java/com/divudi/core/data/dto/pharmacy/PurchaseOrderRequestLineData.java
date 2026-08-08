package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;
import java.math.BigDecimal;

public class PurchaseOrderRequestLineData implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billItemId;
    private Long pharmaceuticalBillItemId;
    private Long itemId;
    private boolean ampp;
    private BigDecimal quantity;
    private BigDecimal freeQuantity;
    private BigDecimal purchaseRate;
    private BigDecimal retailRate;
    private BigDecimal unitsPerPack;
    private int serialNo;
    private Long createrId;

    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }

    public Long getPharmaceuticalBillItemId() { return pharmaceuticalBillItemId; }
    public void setPharmaceuticalBillItemId(Long pharmaceuticalBillItemId) { this.pharmaceuticalBillItemId = pharmaceuticalBillItemId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public boolean isAmpp() { return ampp; }
    public void setAmpp(boolean ampp) { this.ampp = ampp; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getFreeQuantity() { return freeQuantity; }
    public void setFreeQuantity(BigDecimal freeQuantity) { this.freeQuantity = freeQuantity; }

    public BigDecimal getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(BigDecimal purchaseRate) { this.purchaseRate = purchaseRate; }

    public BigDecimal getRetailRate() { return retailRate; }
    public void setRetailRate(BigDecimal retailRate) { this.retailRate = retailRate; }

    public BigDecimal getUnitsPerPack() { return unitsPerPack; }
    public void setUnitsPerPack(BigDecimal unitsPerPack) { this.unitsPerPack = unitsPerPack; }

    public int getSerialNo() { return serialNo; }
    public void setSerialNo(int serialNo) { this.serialNo = serialNo; }

    public Long getCreaterId() { return createrId; }
    public void setCreaterId(Long createrId) { this.createrId = createrId; }
}
