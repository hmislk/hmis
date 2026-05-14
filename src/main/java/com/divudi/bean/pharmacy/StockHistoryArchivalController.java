/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.dto.ArchiveResult;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.archival.StockHistoryArchivalService;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Backing bean for the manual StockHistory archive admin page.
 *
 * Lets an authorised user pick a cutoff date, batch size and dry-run flag,
 * and run a one-off archive pass without waiting for the scheduled job.
 *
 * Issue #20726.
 */
@Named
@SessionScoped
public class StockHistoryArchivalController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private StockHistoryArchivalService archivalService;

    @Inject
    private ConfigOptionApplicationController configOptionController;

    private Date cutoffDate;
    private int batchSize;
    private int maxBatches;
    private boolean dryRun;
    private ArchiveResult lastResult;
    private Long candidateCount;

    public String navigateToArchiveStockHistory() {
        resetDefaults();
        lastResult = null;
        candidateCount = null;
        return "/dataAdmin/archive_stock_history?faces-redirect=true";
    }

    private void resetDefaults() {
        int retentionDays = configOptionController.getIntegerValueByKey(
                "StockHistory Archive - Retention Days", 730);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -retentionDays);
        cutoffDate = cal.getTime();

        batchSize = configOptionController.getIntegerValueByKey(
                "StockHistory Archive - Batch Size", 2000);
        maxBatches = configOptionController.getIntegerValueByKey(
                "StockHistory Archive - Max Batches Per Run", 50);
        dryRun = true;
    }

    public void preview() {
        if (!validateInputs()) {
            return;
        }
        try {
            candidateCount = archivalService.countOlderThan(cutoffDate);
            JsfUtil.addSuccessMessage(candidateCount + " row(s) eligible for archival before "
                    + cutoffDate);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Preview failed: " + ex.getMessage());
        }
    }

    public void run() {
        if (!validateInputs()) {
            return;
        }
        try {
            lastResult = archivalService.archive(cutoffDate, batchSize, maxBatches, dryRun);
            if (dryRun) {
                JsfUtil.addSuccessMessage("Dry run: " + lastResult.getCandidateCount()
                        + " row(s) would be archived");
            } else {
                JsfUtil.addSuccessMessage(lastResult.getMessage());
            }
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Archive run failed: " + ex.getMessage());
        }
    }

    private boolean validateInputs() {
        if (cutoffDate == null) {
            JsfUtil.addErrorMessage("Cutoff date is required");
            return false;
        }
        if (cutoffDate.after(new Date())) {
            JsfUtil.addErrorMessage("Cutoff date cannot be in the future");
            return false;
        }
        if (batchSize <= 0 || batchSize > 50000) {
            JsfUtil.addErrorMessage("Batch size must be between 1 and 50000");
            return false;
        }
        if (maxBatches <= 0 || maxBatches > 1000) {
            JsfUtil.addErrorMessage("Max batches must be between 1 and 1000");
            return false;
        }
        return true;
    }

    public Date getCutoffDate() { return cutoffDate; }
    public void setCutoffDate(Date cutoffDate) { this.cutoffDate = cutoffDate; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public int getMaxBatches() { return maxBatches; }
    public void setMaxBatches(int maxBatches) { this.maxBatches = maxBatches; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public ArchiveResult getLastResult() { return lastResult; }
    public Long getCandidateCount() { return candidateCount; }
}
