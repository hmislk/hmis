package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.util.Date;

/**
 * Lightweight DTO for pending pharmacy transactions that block nursing discharge.
 * Populated via JPQL constructor queries in NursingDischargeController.
 */
public class PendingPharmacyItemDTO {

    private Long billId;
    private String billNumber;
    private Date billDate;
    private BillTypeAtomic billTypeAtomic;

    public PendingPharmacyItemDTO(Long billId, String billNumber, Date billDate, BillTypeAtomic billTypeAtomic) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getPendingTypeLabel() {
        if (billTypeAtomic == null) {
            return "Unknown";
        }
        switch (billTypeAtomic) {
            case REQUEST_MEDICINE_INWARD:
                return "Pharmacy request not fully issued";
            case DIRECT_ISSUE_INWARD_MEDICINE:
            case ISSUE_MEDICINE_ON_REQUEST_INWARD:
                return "Issue not yet accepted by ward";
            case RETURN_MEDICINE_INWARD:
                return "Ward return not yet accepted by pharmacy";
            case DIRECT_ISSUE_INWARD_MEDICINE_RETURN:
                return "Direct-issue return not yet processed";
            default:
                return billTypeAtomic.getLabel();
        }
    }

    public Long getBillId() {
        return billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public Date getBillDate() {
        return billDate;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }
}
