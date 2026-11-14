package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Issuing department group in the Inpatient Direct Issue report (#24760). */
public class InpatientDirectIssueDepartmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long departmentId;
    private String departmentName;
    private List<InpatientDirectIssueBhtDTO> bhts = new ArrayList<>();
    private double totalQty;
    private double totalRetail;
    private double totalCost;

    public InpatientDirectIssueDepartmentDTO() {
    }

    public InpatientDirectIssueDepartmentDTO(Long departmentId, String departmentName) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    public void calculateTotals() {
        totalQty = 0.0;
        totalRetail = 0.0;
        totalCost = 0.0;
        for (InpatientDirectIssueBhtDTO b : bhts) {
            b.calculateTotals();
            totalQty += b.getTotalQty();
            totalRetail += b.getTotalRetail();
            totalCost += b.getTotalCost();
        }
    }

    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public List<InpatientDirectIssueBhtDTO> getBhts() { return bhts; }
    public double getTotalQty() { return totalQty; }
    public double getTotalRetail() { return totalRetail; }
    public double getTotalCost() { return totalCost; }
}
