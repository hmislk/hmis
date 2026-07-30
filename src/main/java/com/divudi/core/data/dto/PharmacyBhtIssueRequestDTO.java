package com.divudi.core.data.dto;

import com.divudi.core.entity.Bill;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * View-layer wrapper for a pharmacy BHT issue-request row
 * (ward_pharmacy_bht_issue_request_list_for_issue.xhtml).
 *
 * This is intentionally a plain Java wrapper constructor, NOT a JPQL
 * SELECT NEW constructor-query projection (see issue #22517). The outer
 * query in SearchController#createInwardBHTForIssueTable must keep
 * returning real Bill entities because they're passed unchanged to
 * PharmacySaleBhtController#checkBillComponentBatch and are needed by the
 * "View Request" / "Issue Medicines" action listeners downstream
 * (pharmacyBillSearch.bill, pharmacySaleBhtController.bhtRequestBill).
 * This DTO removes the deep multi-hop EL navigation from the XHTML while
 * still carrying the original entity via {@link #getBill()}.
 */
public class PharmacyBhtIssueRequestDTO implements Serializable {

    private final Bill bill;
    private final Long billId;
    private final String deptId;
    private final String departmentName;
    private final String bhtNo;
    private final String patientName;
    private final String roomName;
    private final Date requestedAt;
    private final String requestedByName;
    private final boolean cancelled;
    private final Date cancelledAt;
    private final String cancelledByName;
    private List<PharmacyBhtIssuedBillDTO> listOfBill = new ArrayList<>();

    public PharmacyBhtIssueRequestDTO(Bill bill) {
        this.bill = bill;
        this.billId = bill.getId();
        this.deptId = bill.getDeptId();
        this.departmentName = bill.getDepartment() != null ? bill.getDepartment().getName() : null;
        this.bhtNo = (bill.getPatientEncounter() != null) ? bill.getPatientEncounter().getBhtNo() : null;
        this.patientName = (bill.getPatientEncounter() != null && bill.getPatientEncounter().getPatient() != null
                && bill.getPatientEncounter().getPatient().getPerson() != null)
                ? bill.getPatientEncounter().getPatient().getPerson().getNameWithTitle() : null;
        this.roomName = (bill.getPatientEncounter() != null
                && bill.getPatientEncounter().getCurrentPatientRoom() != null
                && bill.getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge() != null
                && bill.getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getRoom() != null)
                ? bill.getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getRoom().getName() : null;
        this.requestedAt = bill.getCreatedAt();
        this.requestedByName = (bill.getCreater() != null && bill.getCreater().getWebUserPerson() != null)
                ? bill.getCreater().getWebUserPerson().getName() : null;
        this.cancelled = bill.isCancelled();
        this.cancelledAt = (bill.isCancelled() && bill.getCancelledBill() != null)
                ? bill.getCancelledBill().getCreatedAt() : null;
        this.cancelledByName = (bill.isCancelled() && bill.getCancelledBill() != null
                && bill.getCancelledBill().getCreater() != null
                && bill.getCancelledBill().getCreater().getWebUserPerson() != null)
                ? bill.getCancelledBill().getCreater().getWebUserPerson().getName() : null;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getRoomName() {
        return roomName;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    public String getRequestedByName() {
        return requestedByName;
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

    public List<PharmacyBhtIssuedBillDTO> getListOfBill() {
        return listOfBill;
    }

    public void setListOfBill(List<PharmacyBhtIssuedBillDTO> listOfBill) {
        this.listOfBill = listOfBill;
    }
}
