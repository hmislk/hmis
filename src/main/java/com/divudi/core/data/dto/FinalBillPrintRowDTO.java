package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One printed line on the "Bundled Custom 1" Final Bill format (issue #23340
 * follow-up: configurable charge-type grouping). Represents either a single
 * ungrouped InwardChargeType's total, or the summed total of every charge
 * type an admin assigned to the same "Final Bill Group" text.
 */
public class FinalBillPrintRowDTO implements Serializable {

    private final String label;
    private final double amount;
    private final int order;

    public FinalBillPrintRowDTO(String label, double amount, int order) {
        this.label = label;
        this.amount = amount;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public double getAmount() {
        return amount;
    }

    public int getOrder() {
        return order;
    }
}
