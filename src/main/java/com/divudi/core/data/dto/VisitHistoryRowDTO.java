package com.divudi.core.data.dto;

import com.divudi.core.data.BillCategory;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.VisitHistoryType;
import java.io.Serializable;
import java.util.Date;

/**
 * One row of the Visit History (issue #25846): a bill or an admission of a
 * patient. Built directly by JPQL constructor queries; carries ids and names,
 * never entities. Two constructors: one for a bill row, one for an admission
 * row.
 */
public class VisitHistoryRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private boolean admission;
    private Date eventDate;
    private VisitHistoryType visitType;
    private String referenceNumber;
    private String documentType;
    private String departmentName;
    private String doctorName;
    private String status;
    /**
     * "Cancellation", "Refund / Return" or empty for an ordinary bill.
     */
    private String kind;
    private boolean internalForPortal;

    public VisitHistoryRowDTO() {
    }

    /**
     * Bill row.
     */
    public VisitHistoryRowDTO(Long billId, String deptId, String insId, Date createdAt, Date billTime,
            BillTypeAtomic billTypeAtomic, DepartmentType departmentType, String departmentName,
            String doctorName, Boolean cancelled, Boolean refunded) {
        this.id = billId;
        this.admission = false;
        this.eventDate = createdAt != null ? createdAt : billTime;
        this.referenceNumber = deptId != null && !deptId.trim().isEmpty() ? deptId : insId;
        this.visitType = VisitHistoryType.resolve(billTypeAtomic, departmentType);
        this.documentType = billTypeAtomic != null ? billTypeAtomic.getLabel() : "Bill";
        this.departmentName = departmentName;
        this.doctorName = doctorName;
        BillCategory cat = billTypeAtomic != null ? billTypeAtomic.getBillCategory() : null;
        if (cat == BillCategory.CANCELLATION) {
            this.kind = "Cancellation";
        } else if (cat == BillCategory.REFUND) {
            this.kind = "Refund / Return";
        } else {
            this.kind = "";
        }
        if (cancelled != null && cancelled) {
            this.status = "Cancelled";
        } else if (refunded != null && refunded) {
            this.status = "Refunded";
        } else {
            this.status = "Active";
        }
        this.internalForPortal = VisitHistoryType.isInternalForPortal(billTypeAtomic);
    }

    /**
     * Admission row.
     */
    public VisitHistoryRowDTO(Long admissionId, String bhtNo, Date admissionDate, Date createdAt, Date dischargeDate,
            String departmentName, String doctorName) {
        this.id = admissionId;
        this.admission = true;
        this.eventDate = admissionDate != null ? admissionDate : createdAt;
        this.referenceNumber = bhtNo;
        this.visitType = VisitHistoryType.INPATIENT;
        this.documentType = "Admission";
        this.departmentName = departmentName;
        this.doctorName = doctorName;
        this.kind = "";
        this.status = dischargeDate != null ? "Discharged" : "Admitted";
        this.internalForPortal = false;
    }

    public Long getId() {
        return id;
    }

    public boolean isAdmission() {
        return admission;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public VisitHistoryType getVisitType() {
        return visitType;
    }

    public String getVisitTypeLabel() {
        return visitType != null ? visitType.getLabel() : "";
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    /**
     * Department and doctor in one display cell.
     */
    public String getDepartmentAndDoctor() {
        boolean hasDept = departmentName != null && !departmentName.trim().isEmpty();
        boolean hasDoc = doctorName != null && !doctorName.trim().isEmpty();
        if (hasDept && hasDoc) {
            return departmentName + " / " + doctorName;
        }
        if (hasDept) {
            return departmentName;
        }
        return hasDoc ? doctorName : "";
    }

    public String getStatus() {
        return status;
    }

    public String getKind() {
        return kind;
    }

    public boolean isCancellationOrRefund() {
        return kind != null && !kind.isEmpty();
    }

    public boolean isInternalForPortal() {
        return internalForPortal;
    }
}
