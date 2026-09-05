package com.divudi.core.data.dto;

import com.divudi.core.entity.Bill;
import java.io.Serializable;
import java.util.Date;

/**
 * View-layer wrapper for a "PharmacyBhtPre" issued bill nested under a
 * pharmacy BHT issue-request row (ward_pharmacy_bht_issue_request_list_for_issue.xhtml).
 *
 * This is intentionally a plain Java wrapper constructor, NOT a JPQL
 * SELECT NEW constructor-query projection. The underlying Bill entity is
 * already loaded (and needed) by SearchController#getBHTIssudBills and is
 * carried through via {@link #getBill()} so downstream action listeners
 * (pharmacyBillSearch.bill) can keep operating on the real entity.
 */
public class PharmacyBhtIssuedBillDTO implements Serializable {

    private final Bill bill;
    private final Long billId;
    private final String deptId;
    private final Date createdAt;
    private final boolean cancelled;
    private final Date cancelledAt;
    private final String cancelledByName;
    private final String issuedByName;
    private final String toStaffName;

    public PharmacyBhtIssuedBillDTO(Bill bill) {
        this.bill = bill;
        this.billId = bill.getId();
        this.deptId = bill.getDeptId();
        this.createdAt = bill.peekCreatedAt();
        this.cancelled = bill.isCancelled();
        this.cancelledAt = (bill.isCancelled() && bill.getCancelledBill() != null)
                ? bill.getCancelledBill().peekCreatedAt() : null;
        this.cancelledByName = (bill.isCancelled() && bill.getCancelledBill() != null
                && bill.getCancelledBill().getCreater() != null
                && bill.getCancelledBill().getCreater().getWebUserPerson() != null)
                ? bill.getCancelledBill().getCreater().getWebUserPerson().getName() : null;
        this.issuedByName = (bill.getCreater() != null && bill.getCreater().getWebUserPerson() != null)
                ? bill.getCreater().getWebUserPerson().getName() : null;
        this.toStaffName = (bill.getToStaff() != null && bill.getToStaff().getPerson() != null)
                ? bill.getToStaff().getPerson().getNameWithTitle() : null;
    }

    public Bill getBill() {
        return bill;
    }

    public Long getBillId() {
        return billId;
    }

    public String getDeptId() {
        return deptId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelledByName() {
        return cancelledByName;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public String getToStaffName() {
        return toStaffName;
    }
}
