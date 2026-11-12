/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.pharmacy.PharmacyBatchChangeException;
import com.divudi.service.pharmacy.PharmacyBatchChangeService;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Batch Change (#17799, Option A): moves this department's quantity of an item
 * from one batch to another existing batch of the same item. The saving is done
 * by {@link PharmacyBatchChangeService} in one transaction.
 */
@Named
@ViewScoped
public class PharmacyBatchChangeController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PRIVILEGE = "PharmacyAdjustmentDepartmentStockQTY";
    private static final String APPROVAL_KEY = "Pharmacy Stock & Price Adjustments - Require Approval";

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private ConfigOptionController configOptionController;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private PharmacyBatchChangeService pharmacyBatchChangeService;

    private Amp amp;
    private List<Stock> stocks;
    private Stock selectedStock;
    private List<SelectItem> targetBatchOptions;
    private Long targetBatchId;
    private Double qty;
    private String comment;
    private Bill bill;
    private boolean printPreview;

    /**
     * Lists this department's batches of the chosen item that have stock.
     */
    public void fillStocks() {
        selectedStock = null;
        targetBatchId = null;
        targetBatchOptions = new ArrayList<>();
        stocks = new ArrayList<>();
        if (amp == null) {
            JsfUtil.addErrorMessage("Select an item.");
            return;
        }
        if (sessionController.getDepartment() == null) {
            JsfUtil.addErrorMessage("No department selected.");
            return;
        }
        String jpql = "select s from Stock s where s.department.id = :did and s.itemBatch.item.id = :iid "
                + "and s.stock > 0 and s.retired = false and s.staff is null order by s.itemBatch.dateOfExpire, s.id";
        Map<String, Object> params = new HashMap<>();
        params.put("did", sessionController.getDepartment().getId());
        params.put("iid", amp.getId());
        stocks = stockFacade.findByJpqlWithoutCache(jpql, params);
        if (stocks == null) {
            stocks = new ArrayList<>();
        }
    }

    /**
     * After a batch row is picked: the "move to" choices are the other batches
     * of the same item.
     */
    public void onStockSelect() {
        targetBatchId = null;
        targetBatchOptions = new ArrayList<>();
        if (selectedStock == null || selectedStock.getItemBatch() == null || selectedStock.getItemBatch().getItem() == null) {
            return;
        }
        String jpql = "select ib from ItemBatch ib where ib.retired = false and ib.item.id = :iid and ib.id <> :sid "
                + "order by ib.dateOfExpire desc, ib.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("iid", selectedStock.getItemBatch().getItem().getId());
        params.put("sid", selectedStock.getItemBatch().getId());
        List<ItemBatch> batches = itemBatchFacade.findByJpqlWithoutCache(jpql, params);
        if (batches == null) {
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("MMM yyyy");
        for (ItemBatch ib : batches) {
            String label = ib.getBatchNo()
                    + " | Exp " + (ib.getDateOfExpire() == null ? "-" : df.format(ib.getDateOfExpire()))
                    + " | Purchase " + String.format("%,.2f", ib.getPurcahseRate())
                    + " | Retail " + String.format("%,.2f", ib.getRetailsaleRate());
            targetBatchOptions.add(new SelectItem(ib.getId(), label));
        }
    }

    /**
     * Saves the batch change. A second call after the bill exists (double
     * click, resubmit) does nothing.
     */
    public synchronized void changeBatch() {
        if (printPreview) {
            return;
        }
        if (!isPermitted()) {
            JsfUtil.addErrorMessage("You are not authorized to change batches.");
            return;
        }
        if (isRequiresApproval()) {
            JsfUtil.addErrorMessage("Batch change is not available while stock adjustments require approval.");
            return;
        }
        if (selectedStock == null) {
            JsfUtil.addErrorMessage("Select the batch to move stock from.");
            return;
        }
        if (targetBatchId == null) {
            JsfUtil.addErrorMessage("Select the batch to move stock to.");
            return;
        }
        if (qty == null || qty <= 0) {
            JsfUtil.addErrorMessage("Quantity to move must be greater than zero.");
            return;
        }
        if (comment == null || comment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Add the comment.");
            return;
        }
        try {
            bill = pharmacyBatchChangeService.changeBatch(selectedStock.getId(), targetBatchId, qty, comment,
                    sessionController.getLoggedUser(), sessionController.getDepartment());
            printPreview = true;
            JsfUtil.addSuccessMessage("Batch changed successfully.");
        } catch (PharmacyBatchChangeException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            Logger.getLogger(PharmacyBatchChangeController.class.getName()).log(Level.SEVERE, "Batch change failed", e);
            JsfUtil.addErrorMessage("The batch change could not be saved. Nothing was changed.");
            return;
        }
        // The save has committed; an expiry-warning lookup failure must not look like a failed save.
        try {
            ItemBatch target = itemBatchFacade.find(targetBatchId);
            if (target != null && target.getDateOfExpire() != null && target.getDateOfExpire().before(new Date())) {
                JsfUtil.addWarningMessage("The batch moved to has already expired.");
            }
        } catch (RuntimeException e) {
            Logger.getLogger(PharmacyBatchChangeController.class.getName()).log(Level.WARNING, "Expiry check after batch change failed", e);
        }
    }

    /**
     * Starts a fresh batch change (clears everything).
     */
    public void startNew() {
        amp = null;
        stocks = null;
        selectedStock = null;
        targetBatchOptions = null;
        targetBatchId = null;
        qty = null;
        comment = null;
        bill = null;
        printPreview = false;
    }

    public boolean isPermitted() {
        return webUserController.hasPrivilege(PRIVILEGE);
    }

    /**
     * Same switch the other adjustments use; batch change has no approval path,
     * so it stays unavailable while that option is on.
     */
    public boolean isRequiresApproval() {
        return configOptionController.getBooleanValueByKey(APPROVAL_KEY, false);
    }

    public Amp getAmp() {
        return amp;
    }

    public void setAmp(Amp amp) {
        this.amp = amp;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public Stock getSelectedStock() {
        return selectedStock;
    }

    public void setSelectedStock(Stock selectedStock) {
        this.selectedStock = selectedStock;
    }

    public List<SelectItem> getTargetBatchOptions() {
        return targetBatchOptions;
    }

    public Long getTargetBatchId() {
        return targetBatchId;
    }

    public void setTargetBatchId(Long targetBatchId) {
        this.targetBatchId = targetBatchId;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Bill getBill() {
        return bill;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }
}
