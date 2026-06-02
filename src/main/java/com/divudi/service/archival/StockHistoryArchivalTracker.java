/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.archival;

import com.divudi.core.data.dto.ArchiveResult;
import java.io.Serializable;
import java.util.Date;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * Application-scoped progress tracker for the StockHistory archival job.
 *
 * Lives outside the HTTP session so that a <p:poll> AJAX request can read
 * progress while the @Asynchronous archival thread is running (session-scoped
 * beans are locked to one request at a time in JSF, making polling impossible
 * during a synchronous run).
 *
 * Issue #20729.
 */
@Named
@ApplicationScoped
public class StockHistoryArchivalTracker implements Serializable {

    private static final long serialVersionUID = 1L;

    private volatile boolean running = false;
    private volatile long candidates = 0;
    private volatile long archivedSoFar = 0;
    private volatile int batchesDone = 0;
    private volatile int maxBatches = 0;
    private volatile Date startedAt;
    private volatile ArchiveResult lastResult;

    public synchronized void start(long candidates, int maxBatches) {
        this.candidates = candidates;
        this.archivedSoFar = 0;
        this.batchesDone = 0;
        this.maxBatches = maxBatches;
        this.startedAt = new Date();
        this.lastResult = null;
        this.running = true;
    }

    public synchronized void recordBatch(int moved) {
        archivedSoFar += moved;
        batchesDone++;
    }

    public synchronized void finish(ArchiveResult result) {
        this.lastResult = result;
        this.running = false;
    }

    public int getProgressPercent() {
        long c = candidates;
        long a = archivedSoFar;
        if (c <= 0) return 0;
        return (int) Math.min(100, (a * 100L) / c);
    }

    public boolean isRunning() { return running; }
    public long getCandidates() { return candidates; }
    public long getArchivedSoFar() { return archivedSoFar; }
    public int getBatchesDone() { return batchesDone; }
    public int getMaxBatches() { return maxBatches; }
    public Date getStartedAt() { return startedAt; }
    public ArchiveResult getLastResult() { return lastResult; }
}
