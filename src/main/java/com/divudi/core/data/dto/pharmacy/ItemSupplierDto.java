package com.divudi.core.data.dto.pharmacy;

import com.divudi.core.entity.pharmacy.ItemsDistributors;
import java.io.Serializable;
import java.util.Date;

/**
 * Data transfer object representing an item-supplier link (ItemsDistributors).
 */
public class ItemSupplierDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long itemId;
    private String itemName;
    private String itemCode;
    private int orderNo;
    private boolean retired;
    private Date createdAt;
    private Long createrId;
    private String createrName;
    private Date retiredAt;
    private Long retirerId;
    private String retirerName;
    private String retireComments;

    public ItemSupplierDto() {
    }

    public static ItemSupplierDto fromEntity(ItemsDistributors entity) {
        if (entity == null) {
            return null;
        }
        ItemSupplierDto dto = new ItemSupplierDto();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setRetired(entity.isRetired());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setRetiredAt(entity.getRetiredAt());
        dto.setRetireComments(entity.getRetireComments());

        if (entity.getInstitution() != null) {
            dto.setSupplierId(entity.getInstitution().getId());
            dto.setSupplierName(entity.getInstitution().getName());
        }

        if (entity.getItem() != null) {
            dto.setItemId(entity.getItem().getId());
            dto.setItemName(entity.getItem().getName());
            dto.setItemCode(entity.getItem().getCode());
        }

        if (entity.getCreater() != null) {
            dto.setCreaterId(entity.getCreater().getId());
            if (entity.getCreater().getWebUserPerson() != null && entity.getCreater().getWebUserPerson().getName() != null) {
                dto.setCreaterName(entity.getCreater().getWebUserPerson().getName());
            } else {
                dto.setCreaterName(entity.getCreater().getName());
            }
        }

        if (entity.getRetirer() != null) {
            dto.setRetirerId(entity.getRetirer().getId());
            if (entity.getRetirer().getWebUserPerson() != null && entity.getRetirer().getWebUserPerson().getName() != null) {
                dto.setRetirerName(entity.getRetirer().getWebUserPerson().getName());
            } else {
                dto.setRetirerName(entity.getRetirer().getName());
            }
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreaterId() {
        return createrId;
    }

    public void setCreaterId(Long createrId) {
        this.createrId = createrId;
    }

    public String getCreaterName() {
        return createrName;
    }

    public void setCreaterName(String createrName) {
        this.createrName = createrName;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public Long getRetirerId() {
        return retirerId;
    }

    public void setRetirerId(Long retirerId) {
        this.retirerId = retirerId;
    }

    public String getRetirerName() {
        return retirerName;
    }

    public void setRetirerName(String retirerName) {
        this.retirerName = retirerName;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }
}
