package com.divudi.core.data;

import java.io.Serializable;

/**
 * A search result offered by the combined institution/person recipient
 * autocomplete on the inpatient email compose page. Not persisted - only
 * the resolved email address is stored once picked.
 */
public class EmailRecipientCandidate implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label;
    private String email;
    private String sourceType;

    public EmailRecipientCandidate() {
    }

    public EmailRecipientCandidate(String label, String email, String sourceType) {
        this.label = label;
        this.email = email;
        this.sourceType = sourceType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public String toString() {
        return label + " <" + email + ">";
    }
}
