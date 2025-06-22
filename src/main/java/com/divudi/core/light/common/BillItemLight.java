package com.divudi.core.light.common;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;

public class BillItemLight {

    private Institution institution;
    private Department department;
    private Item item;
    private BillTypeAtomic billTypeAtomic;
    private Double qty;
    private Double freeQty;
    private Double netTotal;

    public BillItemLight() {
    }

    public BillItemLight(Institution institution, Department department, Item item,
            BillTypeAtomic billTypeAtomic, Double qty, Double freeQty, Double netTotal) {
        this.institution = institution;
        this.department = department;
        this.item = item;
        this.billTypeAtomic = billTypeAtomic;
        this.qty = qty;
        this.freeQty = freeQty;
        this.netTotal = netTotal;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(Double freeQty) {
        this.freeQty = freeQty;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }
}
