/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Result of an archival run.
 *
 * Used by ArchivalServiceBase (issue #20726) and the manual admin trigger
 * so the caller can see how much was moved without having to query the
 * archive table directly.
 */
public class ArchiveResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean dryRun;
    private final long candidateCount;
    private final long archivedCount;
    private final int batchesRun;
    private final boolean reachedBatchLimit;
    private final Date startedAt;
    private final Date endedAt;
    private final String message;

    public ArchiveResult(boolean dryRun, long candidateCount, long archivedCount,
                         int batchesRun, boolean reachedBatchLimit,
                         Date startedAt, Date endedAt, String message) {
        this.dryRun = dryRun;
        this.candidateCount = candidateCount;
        this.archivedCount = archivedCount;
        this.batchesRun = batchesRun;
        this.reachedBatchLimit = reachedBatchLimit;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.message = message;
    }

    public boolean isDryRun() { return dryRun; }
    public long getCandidateCount() { return candidateCount; }
    public long getArchivedCount() { return archivedCount; }
    public int getBatchesRun() { return batchesRun; }
    public boolean isReachedBatchLimit() { return reachedBatchLimit; }
    public Date getStartedAt() { return startedAt; }
    public Date getEndedAt() { return endedAt; }
    public String getMessage() { return message; }

    public long getDurationMillis() {
        if (startedAt == null || endedAt == null) return 0;
        return endedAt.getTime() - startedAt.getTime();
    }
}
