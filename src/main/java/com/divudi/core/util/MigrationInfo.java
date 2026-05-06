/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.util;

import java.util.Date;
import java.util.List;

/**
 * Data class for migration metadata parsed from migration-info.json
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public class MigrationInfo {
    private String version;
    private String description;
    private String author;
    private Date date;
    private String githubIssue;
    private String branch;
    private boolean requiresDowntime;
    private int estimatedDurationMinutes;
    private List<String> affectedTables;
    private List<String> affectedModules;
    private List<String> dependencies;
    private List<String> preRequisites;
    private List<String> postMigrationVerification;
    private List<String> rollbackNotes;
    private List<MigrationStep> migrationSteps;
    private List<String> verificationQueries;

    // Inner class for migration steps
    public static class MigrationStep {
        private int step;
        private String description;
        private String sql;

        // Constructors
        public MigrationStep() {}

        public MigrationStep(int step, String description, String sql) {
            this.step = step;
            this.description = description;
            this.sql = sql;
        }

        // Getters and Setters
        public int getStep() {
            return step;
        }

        public void setStep(int step) {
            this.step = step;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        @Override
        public String toString() {
            return "Step " + step + ": " + description;
        }
    }

    // Constructors
    public MigrationInfo() {}

    public MigrationInfo(String version, String description) {
        this.version = version;
        this.description = description;
    }

    // Getters and Setters
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getGithubIssue() {
        return githubIssue;
    }

    public void setGithubIssue(String githubIssue) {
        this.githubIssue = githubIssue;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public boolean isRequiresDowntime() {
        return requiresDowntime;
    }

    public void setRequiresDowntime(boolean requiresDowntime) {
        this.requiresDowntime = requiresDowntime;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public List<String> getAffectedTables() {
        return affectedTables;
    }

    public void setAffectedTables(List<String> affectedTables) {
        this.affectedTables = affectedTables;
    }

    public List<String> getAffectedModules() {
        return affectedModules;
    }

    public void setAffectedModules(List<String> affectedModules) {
        this.affectedModules = affectedModules;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getPreRequisites() {
        return preRequisites;
    }

    public void setPreRequisites(List<String> preRequisites) {
        this.preRequisites = preRequisites;
    }

    public List<String> getPostMigrationVerification() {
        return postMigrationVerification;
    }

    public void setPostMigrationVerification(List<String> postMigrationVerification) {
        this.postMigrationVerification = postMigrationVerification;
    }

    public List<String> getRollbackNotes() {
        return rollbackNotes;
    }

    public void setRollbackNotes(List<String> rollbackNotes) {
        this.rollbackNotes = rollbackNotes;
    }

    public List<MigrationStep> getMigrationSteps() {
        return migrationSteps;
    }

    public void setMigrationSteps(List<MigrationStep> migrationSteps) {
        this.migrationSteps = migrationSteps;
    }

    public List<String> getVerificationQueries() {
        return verificationQueries;
    }

    public void setVerificationQueries(List<String> verificationQueries) {
        this.verificationQueries = verificationQueries;
    }

    // Utility methods
    public long getEstimatedDurationMs() {
        return estimatedDurationMinutes * 60L * 1000L;
    }

    public boolean hasPreRequisites() {
        return preRequisites != null && !preRequisites.isEmpty();
    }

    public boolean hasVerificationQueries() {
        return verificationQueries != null && !verificationQueries.isEmpty();
    }

    public boolean hasMigrationSteps() {
        return migrationSteps != null && !migrationSteps.isEmpty();
    }

    public String getAffectedTablesDisplay() {
        if (affectedTables == null || affectedTables.isEmpty()) {
            return "None specified";
        }
        return String.join(", ", affectedTables);
    }

    public String getAffectedModulesDisplay() {
        if (affectedModules == null || affectedModules.isEmpty()) {
            return "None specified";
        }
        return String.join(", ", affectedModules);
    }

    @Override
    public String toString() {
        return "MigrationInfo{" +
                "version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", author='" + author + '\'' +
                ", requiresDowntime=" + requiresDowntime +
                ", estimatedDurationMinutes=" + estimatedDurationMinutes +
                '}';
    }
}