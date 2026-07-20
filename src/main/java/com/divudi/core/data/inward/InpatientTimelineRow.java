/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 */
package com.divudi.core.data.inward;

import com.divudi.core.entity.AuditEvent;
import java.io.Serializable;
import java.util.Date;

/**
 * One row of the Inpatient Event History timeline (#22240) — either a bill
 * (billing-traceable event) or an AuditEvent (non-billing action), merged
 * chronologically to tell the full story of an admission.
 *
 * @author Dr M H B Ariyaratne
 */
public class InpatientTimelineRow implements Serializable, Comparable<InpatientTimelineRow> {

    private static final long serialVersionUID = 1L;

    private final Date when;
    private final String category;
    private final String description;
    private final String user;
    private final Long billId;
    private final Double amount;
    private final AuditEvent auditEvent;

    public InpatientTimelineRow(Date when, String category, String description,
            String user, Long billId, Double amount, AuditEvent auditEvent) {
        this.when = when;
        this.category = category;
        this.description = description;
        this.user = user;
        this.billId = billId;
        this.amount = amount;
        this.auditEvent = auditEvent;
    }

    @Override
    public int compareTo(InpatientTimelineRow other) {
        if (when == null && (other == null || other.when == null)) {
            return 0;
        }
        if (when == null) {
            return -1;
        }
        if (other == null || other.when == null) {
            return 1;
        }
        return when.compareTo(other.when);
    }

    /**
     * Unique row key for the PrimeFaces dataTable (required by rowExpansion).
     */
    public String getKey() {
        if (billId != null) {
            return "B" + billId;
        }
        if (auditEvent != null && auditEvent.getId() != null) {
            return "A" + auditEvent.getId();
        }
        return "R" + System.identityHashCode(this);
    }

    public boolean isBillRow() {
        return billId != null;
    }

    public boolean isAuditRow() {
        return auditEvent != null;
    }

    public Date getWhen() {
        return when;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getUser() {
        return user;
    }

    public Long getBillId() {
        return billId;
    }

    public Double getAmount() {
        return amount;
    }

    public AuditEvent getAuditEvent() {
        return auditEvent;
    }

}
