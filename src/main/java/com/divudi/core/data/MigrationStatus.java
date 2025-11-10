/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data;

/**
 * Enum for tracking database migration execution status
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public enum MigrationStatus {
    /**
     * Migration is pending execution
     */
    PENDING("Pending", "Migration is waiting to be executed"),

    /**
     * Migration is currently being executed
     */
    EXECUTING("Executing", "Migration is currently running"),

    /**
     * Migration completed successfully
     */
    SUCCESS("Success", "Migration completed successfully"),

    /**
     * Migration failed during execution
     */
    FAILED("Failed", "Migration failed during execution"),

    /**
     * Migration was rolled back
     */
    ROLLED_BACK("Rolled Back", "Migration was successfully rolled back"),

    /**
     * Rollback failed
     */
    ROLLBACK_FAILED("Rollback Failed", "Migration rollback failed");

    private final String label;
    private final String description;

    MigrationStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this status indicates a completed state (success or failure)
     */
    public boolean isCompleted() {
        return this == SUCCESS || this == FAILED || this == ROLLED_BACK || this == ROLLBACK_FAILED;
    }

    /**
     * Check if this status indicates an active state (currently executing)
     */
    public boolean isActive() {
        return this == EXECUTING;
    }

    /**
     * Check if this status indicates a successful completion
     */
    public boolean isSuccessful() {
        return this == SUCCESS;
    }

    /**
     * Check if this status indicates a failure
     */
    public boolean isFailed() {
        return this == FAILED || this == ROLLBACK_FAILED;
    }

    /**
     * Get CSS class for UI styling based on status
     */
    public String getCssClass() {
        switch (this) {
            case SUCCESS:
                return "text-success";
            case FAILED:
            case ROLLBACK_FAILED:
                return "text-danger";
            case EXECUTING:
                return "text-info";
            case ROLLED_BACK:
                return "text-warning";
            case PENDING:
            default:
                return "text-muted";
        }
    }

    /**
     * Get icon class for UI display based on status
     */
    public String getIconClass() {
        switch (this) {
            case SUCCESS:
                return "fa fa-check-circle";
            case FAILED:
            case ROLLBACK_FAILED:
                return "fa fa-exclamation-circle";
            case EXECUTING:
                return "fa fa-spinner fa-spin";
            case ROLLED_BACK:
                return "fa fa-undo";
            case PENDING:
            default:
                return "fa fa-clock-o";
        }
    }
}