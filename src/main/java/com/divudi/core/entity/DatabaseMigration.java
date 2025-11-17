/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import com.divudi.core.data.MigrationStatus;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entity to track database migration execution history
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
@Entity
public class DatabaseMigration implements Serializable, Comparable<DatabaseMigration> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String version;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MigrationStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date executedAt;

    @ManyToOne
    private WebUser executedBy;

    @Column(nullable = true)
    private Long executionTimeMs;

    @Lob
    private String errorMessage;

    @Lob
    private String executionLog;

    @Column(nullable = true, length = 100)
    private String rollbackFilename;

    @Temporal(TemporalType.TIMESTAMP)
    private Date rollbackAt;

    @ManyToOne
    private WebUser rollbackBy;

    @Column(nullable = false)
    private boolean requiresDowntime = false;

    @Column(nullable = true)
    private Long estimatedDurationMs;

    @Lob
    private String migrationMetadata; // JSON metadata from migration-info.json

    // Constructors
    public DatabaseMigration() {
        this.executedAt = new Date();
        this.status = MigrationStatus.PENDING;
    }

    public DatabaseMigration(String version, String description, String filename) {
        this();
        this.version = version;
        this.description = description;
        this.filename = filename;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public MigrationStatus getStatus() {
        return status;
    }

    public void setStatus(MigrationStatus status) {
        this.status = status;
    }

    public Date getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Date executedAt) {
        this.executedAt = executedAt;
    }

    public WebUser getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(WebUser executedBy) {
        this.executedBy = executedBy;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getExecutionLog() {
        return executionLog;
    }

    public void setExecutionLog(String executionLog) {
        this.executionLog = executionLog;
    }

    public String getRollbackFilename() {
        return rollbackFilename;
    }

    public void setRollbackFilename(String rollbackFilename) {
        this.rollbackFilename = rollbackFilename;
    }

    public Date getRollbackAt() {
        return rollbackAt;
    }

    public void setRollbackAt(Date rollbackAt) {
        this.rollbackAt = rollbackAt;
    }

    public WebUser getRollbackBy() {
        return rollbackBy;
    }

    public void setRollbackBy(WebUser rollbackBy) {
        this.rollbackBy = rollbackBy;
    }

    public boolean isRequiresDowntime() {
        return requiresDowntime;
    }

    public void setRequiresDowntime(boolean requiresDowntime) {
        this.requiresDowntime = requiresDowntime;
    }

    public Long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }

    public void setEstimatedDurationMs(Long estimatedDurationMs) {
        this.estimatedDurationMs = estimatedDurationMs;
    }

    public String getMigrationMetadata() {
        return migrationMetadata;
    }

    public void setMigrationMetadata(String migrationMetadata) {
        this.migrationMetadata = migrationMetadata;
    }

    // Utility methods
    public boolean isSuccessful() {
        return status == MigrationStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == MigrationStatus.FAILED;
    }

    public boolean isPending() {
        return status == MigrationStatus.PENDING;
    }

    public boolean isExecuting() {
        return status == MigrationStatus.EXECUTING;
    }

    public boolean isRolledBack() {
        return status == MigrationStatus.ROLLED_BACK;
    }

    public String getExecutionTimeDisplay() {
        if (executionTimeMs == null) {
            return "N/A";
        }
        if (executionTimeMs < 1000) {
            return executionTimeMs + "ms";
        } else if (executionTimeMs < 60000) {
            return String.format("%.1fs", executionTimeMs / 1000.0);
        } else {
            return String.format("%.1fm", executionTimeMs / 60000.0);
        }
    }

    @Override
    public int compareTo(DatabaseMigration other) {
        if (other == null) {
            return 1;
        }
        // Compare by version (semantic versioning)
        return compareVersions(this.version, other.version);
    }

    private int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) {
            return 0;
        }
        if (version1 == null) {
            return -1;
        }
        if (version2 == null) {
            return 1;
        }

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    @Override
    public int hashCode() {
        return version != null ? version.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DatabaseMigration other = (DatabaseMigration) obj;
        return version != null ? version.equals(other.version) : other.version == null;
    }

    @Override
    public String toString() {
        return "DatabaseMigration{" +
                "version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", executedAt=" + executedAt +
                '}';
    }
}