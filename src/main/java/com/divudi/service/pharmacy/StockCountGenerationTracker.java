package com.divudi.service.pharmacy;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javax.ejb.Singleton;

/**
 * Tracks progress of async stock count bill generation jobs.
 * Singleton EJB to maintain state across async method calls.
 */
@Singleton
public class StockCountGenerationTracker implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class Progress implements Serializable {
        private static final long serialVersionUID = 1L;

        public int totalItems;
        public int processedItems;
        public String status;
        public boolean completed;
        public boolean failed;
        public String errorMessage;
        public Date startedAt;
        public Date finishedAt;
        public Long billId; // ID of the generated snapshot bill
    }

    private final ConcurrentHashMap<String, Progress> jobs = new ConcurrentHashMap<>();

    public void start(String jobId, int totalItems, String status) {
        Progress p = new Progress();
        p.totalItems = Math.max(totalItems, 0);
        p.processedItems = 0;
        p.status = status;
        p.completed = false;
        p.failed = false;
        p.startedAt = new Date();
        jobs.put(jobId, p);
    }

    public void updateProgress(String jobId, int processed, String status) {
        Progress p = jobs.get(jobId);
        if (p == null) return;
        p.processedItems = processed;
        if (status != null) {
            p.status = status;
        }
    }

    public void complete(String jobId, Long billId) {
        Progress p = jobs.get(jobId);
        if (p == null) return;
        p.completed = true;
        p.finishedAt = new Date();
        p.billId = billId;
        p.status = "Stock count bill generated successfully";
    }

    public void fail(String jobId, String errorMessage) {
        Progress p = jobs.get(jobId);
        if (p == null) return;
        p.failed = true;
        p.finishedAt = new Date();
        p.status = "Failed";
        p.errorMessage = errorMessage;
    }

    public Progress get(String jobId) {
        return jobs.get(jobId);
    }

    public void remove(String jobId) {
        jobs.remove(jobId);
    }
}
