package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Bill;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 */
public class BillAndItemDataRow {

    private Bill bill;
    private UUID id;
    private List<ItemDetailsCell> itemDetailCells;
    private double grandTotal;

    public BillAndItemDataRow() {
        id = UUID.randomUUID();
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public List<ItemDetailsCell> getItemDetailCells() {
        if(itemDetailCells==null){
            itemDetailCells = new ArrayList<>();
        }
        return itemDetailCells;
    }

    public void setItemDetailCells(List<ItemDetailsCell> itemDetailCells) {
        this.itemDetailCells = itemDetailCells;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BillAndItemDataRow other = (BillAndItemDataRow) obj;
        return Objects.equals(this.id, other.id);
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(double grandTotal) {
        this.grandTotal = grandTotal;
    }



}
