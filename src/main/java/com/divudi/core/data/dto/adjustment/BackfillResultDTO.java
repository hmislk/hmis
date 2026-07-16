package com.divudi.core.data.dto.adjustment;

public class BackfillResultDTO {
    private Long billId;
    private String billTypeAtomic;
    private double computedNetTotal;
    private double computedTotal;
    private boolean applied;
    private String note;

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getBillTypeAtomic() { return billTypeAtomic; }
    public void setBillTypeAtomic(String billTypeAtomic) { this.billTypeAtomic = billTypeAtomic; }

    public double getComputedNetTotal() { return computedNetTotal; }
    public void setComputedNetTotal(double computedNetTotal) { this.computedNetTotal = computedNetTotal; }

    public double getComputedTotal() { return computedTotal; }
    public void setComputedTotal(double computedTotal) { this.computedTotal = computedTotal; }

    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
