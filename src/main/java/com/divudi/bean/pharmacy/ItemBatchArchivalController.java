/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.dto.ArchiveResult;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.archival.ItemBatchArchivalService;
import com.divudi.service.archival.ItemBatchArchivalTracker;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Backing bean for the manual ItemBatch archive admin page.
 *
 * Lets an authorised user pick a cutoff date, batch size and dry-run flag,
 * and run a one-off archive pass without waiting for the scheduled job.
 *
 * Issue #20724.
 */
@Named
@SessionScoped
public class ItemBatchArchivalController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(ItemBatchArchivalController.class.getName());

    @EJB
    private ItemBatchArchivalService archivalService;

    @Inject
    private ConfigOptionApplicationController configOptionController;

    @Inject
    private ItemBatchArchivalTracker tracker;

    private Date cutoffDate;
    private int batchSize;
    private int maxBatches;
    private boolean dryRun;
    private Long candidateCount;

    public String navigateToArchiveItemBatch() {
        resetDefaults();
        candidateCount = null;
        return "/dataAdmin/archive_item_batch?faces-redirect=true";
    }

    private void resetDefaults() {
        int retentionDays = sanitisePositive(configOptionController.getIntegerValueByKey(
                "ItemBatch Archive - Retention Days", 365), 365);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -retentionDays);
        cutoffDate = cal.getTime();

        batchSize = sanitisePositive(configOptionController.getIntegerValueByKey(
                "ItemBatch Archive - Batch Size", 1000), 1000);
        maxBatches = sanitisePositive(configOptionController.getIntegerValueByKey(
                "ItemBatch Archive - Max Batches Per Run", 20), 20);
        dryRun = true;
    }

    private static int sanitisePositive(Integer raw, int fallback) {
        return (raw == null || raw <= 0) ? fallback : raw;
    }

    public void preview() {
        if (!validateInputs()) {
            return;
        }
        try {
            candidateCount = archivalService.countOlderThan(cutoffDate);
            JsfUtil.addSuccessMessage(candidateCount + " ItemBatch row(s) eligible for archival before "
                    + cutoffDate);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "ItemBatch archival preview failed", ex);
            JsfUtil.addErrorMessage("Preview failed. Please check server logs for details.");
        }
    }

    public void run() {
        if (!validateInputs()) {
            return;
        }
        if (tracker.isRunning()) {
            JsfUtil.addErrorMessage("Archival is already in progress.");
            return;
        }
        try {
            if (dryRun) {
                ArchiveResult result = archivalService.archive(cutoffDate, batchSize, maxBatches, true);
                tracker.start(result.getCandidateCount(), 0);
                tracker.finish(result);
                JsfUtil.addSuccessMessage("Dry run: " + result.getCandidateCount()
                        + " ItemBatch row(s) would be archived");
            } else {
                archivalService.archiveAsync(cutoffDate, batchSize, maxBatches);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "ItemBatch archive run failed", ex);
            JsfUtil.addErrorMessage("Archive run failed. Please check server logs for details.");
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

    public ArchiveResult getLastResult() { return tracker.getLastResult(); }
    public ItemBatchArchivalTracker getTracker() { return tracker; }
    public Long getCandidateCount() { return candidateCount; }
}
