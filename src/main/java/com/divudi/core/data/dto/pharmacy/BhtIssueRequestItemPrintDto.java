package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;

public class BhtIssueRequestItemPrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemName = "";
    private double qty;
    private String directions = "";

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public String getDirections() {
        return directions;
    }

    public void setDirections(String directions) {
        this.directions = directions;
    }
}
