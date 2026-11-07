package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Items of one admission (BHT) in the Inpatient Direct Issue report (#24760). */
public class InpatientDirectIssueBhtDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long encounterId;
    private String bhtNo;
    private String patientName;
    private String patientPhn;
    private List<InpatientDirectIssueItemDTO> items = new ArrayList<>();
    private double totalQty;
    private double totalRetail;
    private double totalCost;

    public InpatientDirectIssueBhtDTO() {
    }

    public InpatientDirectIssueBhtDTO(Long encounterId, String bhtNo, String patientName, String patientPhn) {
        this.encounterId = encounterId;
        this.bhtNo = bhtNo;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
    }

    public void calculateTotals() {
        totalQty = 0.0;
        totalRetail = 0.0;
        totalCost = 0.0;
        for (InpatientDirectIssueItemDTO i : items) {
            totalQty += i.getSignedQty();
            totalRetail += i.getRetailValue();
            totalCost += i.getCostValue();
        }
    }

    public Long getEncounterId() { return encounterId; }
    public String getBhtNo() { return bhtNo; }
    public String getPatientName() { return patientName; }
    public String getPatientPhn() { return patientPhn; }
    public List<InpatientDirectIssueItemDTO> getItems() { return items; }
    public double getTotalQty() { return totalQty; }
    public double getTotalRetail() { return totalRetail; }
    public double getTotalCost() { return totalCost; }
}
