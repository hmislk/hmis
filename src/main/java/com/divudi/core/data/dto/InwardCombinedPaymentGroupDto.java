package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A group of rows on the Combined Inward Payments report, with its subtotal.
 *
 * The grouping key depends on the selected view: the bill type in "Grouped by
 * Type", the BHT number in "Grouped by BHT". In "Summary" only the label, the
 * count and the total are printed - the rows are still carried so the same
 * structure serves every grouped view.
 */
public class InwardCombinedPaymentGroupDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String groupLabel;
    private List<InwardCombinedPaymentRowDto> rows = new ArrayList<>();
    private double total;
    private double cashIn;
    private double cashOut;

    public InwardCombinedPaymentGroupDto() {
    }

    public InwardCombinedPaymentGroupDto(String groupLabel) {
        this.groupLabel = groupLabel;
    }

    public void add(InwardCombinedPaymentRowDto row) {
        rows.add(row);
        double signed = row.getSignedAmount();
        total += signed;
        if (signed < 0) {
            cashOut += signed;
        } else {
            cashIn += signed;
        }
    }

    public int getCount() {
        return rows.size();
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public void setGroupLabel(String groupLabel) {
        this.groupLabel = groupLabel;
    }

    public List<InwardCombinedPaymentRowDto> getRows() {
        return rows;
    }

    public void setRows(List<InwardCombinedPaymentRowDto> rows) {
        this.rows = rows;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getCashIn() {
        return cashIn;
    }

    public void setCashIn(double cashIn) {
        this.cashIn = cashIn;
    }

    public double getCashOut() {
        return cashOut;
    }

    public void setCashOut(double cashOut) {
        this.cashOut = cashOut;
    }
}
