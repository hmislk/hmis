package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One technician's rows on the Inward Technician Charges Report (issue #25818),
 * with the Added / Paid Fee Value subtotals.
 */
public class InwardTechnicianChargeGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String technicianName;
    private final List<InwardTechnicianChargeRowDTO> rows = new ArrayList<>();
    private double addedFeeTotal;
    private double paidFeeTotal;

    public InwardTechnicianChargeGroupDTO(String technicianName) {
        this.technicianName = technicianName;
    }

    public void add(InwardTechnicianChargeRowDTO row) {
        rows.add(row);
        addedFeeTotal += row.getAddedFeeValue();
        paidFeeTotal += row.getPaidFeeValue();
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public List<InwardTechnicianChargeRowDTO> getRows() {
        return rows;
    }

    public double getAddedFeeTotal() {
        return addedFeeTotal;
    }

    public double getPaidFeeTotal() {
        return paidFeeTotal;
    }
}
