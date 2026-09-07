package com.divudi.service.pharmacy;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import javax.ejb.Singleton;

@Singleton
public class ApprovalProgressTracker implements Serializable {

    public static class Progress implements Serializable {
        public int total;
        public int processed;
        public String status;
        public boolean completed;
        public boolean failed;
        public String errorMessage;
        public Date startedAt;
        public Date finishedAt;
    }

    private final ConcurrentHashMap<String, Progress> jobs = new ConcurrentHashMap<>();

    public void start(String jobId, int total, String status) {
        Progress p = new Progress();
        p.total = Math.max(total, 0);
        p.processed = 0;
        p.status = status;
        p.completed = false;
        p.failed = false;
        p.startedAt = new Date();
        jobs.put(jobId, p);
    }

    public void step(String jobId, int processed, String status) {
        Progress p = jobs.get(jobId);
        if (p == null) return;
        p.processed = processed;
        if (status != null) {
            p.status = status;
        }
    }

    public void complete(String jobId) {
        Progress p = jobs.get(jobId);
        if (p == null) return;
        p.completed = true;
        p.finishedAt = new Date();
        p.status = p.status == null ? "Completed" : p.status;
    }

    public void fail(String jobId, String message) {
        Progress p = jobs.get(jobId);
        if (p == null) return;
        p.failed = true;
        p.finishedAt = new Date();
        p.status = "Failed";
        p.errorMessage = message;
    }

    public Progress get(String jobId) {
        return jobs.get(jobId);
    }
}

