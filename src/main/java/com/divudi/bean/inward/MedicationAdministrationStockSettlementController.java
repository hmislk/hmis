package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.MedicationAdministrationStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.clinical.MedicationAdministrationRecord;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.MedicationAdministrationRecordFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Stage 2 of #21469 (part of #21466): batch-deducts ward stock for one or
 * more {@link MedicationAdministrationRecord}s that were already recorded as
 * {@link MedicationAdministrationStatus#GIVEN} but not yet deducted from ward
 * stock (Stage 1, {@link MedicationAdministrationController}).
 *
 * <p>Records sharing the same item, batch and patient encounter are
 * aggregated into a single {@link BillItem} of a
 * {@link BillTypeAtomic#WARD_MEDICINE_ADMINISTRATION_CONSUMPTION} bill, and
 * ward stock is deducted once per aggregated line.</p>
 */
@Named
@SessionScoped
public class MedicationAdministrationStockSettlementController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private MedicationAdministrationRecordFacade medicationAdministrationRecordFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillNumberGenerator billNumberBean;

    @Inject
    private SessionController sessionController;

    private List<MedicationAdministrationRecord> pendingRecords;
    private MedicationAdministrationRecord[] selectedRecords;
    private List<BillItem> lastSettledItems;
    private Bill lastSettlementBill;
    private boolean printPreview;
    private boolean settling;

    public String navigateToSettle() {
        printPreview = false;
        lastSettlementBill = null;
        lastSettledItems = null;
        selectedRecords = null;
        loadPendingRecords();
        return "/ward/ward_medication_administration_settle?faces-redirect=true";
    }

    public void loadPendingRecords() {
        String jpql = "SELECT m FROM MedicationAdministrationRecord m WHERE m.status = :given "
                + "AND m.stockDeducted = false AND m.retired = false AND m.department = :dept "
                + "ORDER BY m.administeredAt ASC";
        Map<String, Object> params = new HashMap<>();
        params.put("given", MedicationAdministrationStatus.GIVEN);
        params.put("dept", sessionController.getDepartment());
        pendingRecords = medicationAdministrationRecordFacade.findByJpql(jpql, params);
        if (pendingRecords == null) {
            pendingRecords = new ArrayList<>();
        }
    }

    public void settle() {
        if (settling) {
            return;
        }
        settling = true;
        try {
            doSettle();
        } finally {
            settling = false;
        }
    }

    private void doSettle() {
        printPreview = false;
        if (selectedRecords == null || selectedRecords.length == 0) {
            JsfUtil.addErrorMessage("Select at least one administration record.");
            return;
        }

        Map<String, List<MedicationAdministrationRecord>> groups = new LinkedHashMap<>();
        for (MedicationAdministrationRecord m : selectedRecords) {
            if (m.getItemBatch() == null || m.getItemBatch().getId() == null) {
                JsfUtil.addErrorMessage("Record for " + (m.getItem() != null ? m.getItem().getName() : "?") + " has no batch - cannot settle.");
                return;
            }
            String key = m.getItem().getId() + "_" + m.getItemBatch().getId() + "_"
                    + (m.getPatientEncounter() != null ? m.getPatientEncounter().getId() : "null");
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        }

        if (!wardStockCoversAllLines(groups)) {
            return;
        }

        BilledBill bill = new BilledBill();
        bill.setBillType(BillType.PharmacyBhtPre);
        bill.setBillTypeAtomic(BillTypeAtomic.WARD_MEDICINE_ADMINISTRATION_CONSUMPTION);
        bill.setInstitution(sessionController.getInstitution());
        bill.setDepartment(sessionController.getDepartment());
        bill.setCreatedAt(new Date());
        bill.setCreater(sessionController.getLoggedUser());

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.WARD_MEDICINE_ADMINISTRATION_CONSUMPTION);
        bill.setDeptId(deptId);
        bill.setInsId(deptId);

        billFacade.create(bill);

        List<BillItem> settledItems = new ArrayList<>();
        int serial = 1;
        for (List<MedicationAdministrationRecord> recs : groups.values()) {
            MedicationAdministrationRecord sample = recs.get(0);
            double totalQty = 0.0;
            for (MedicationAdministrationRecord m : recs) {
                totalQty += m.getQty();
            }

            BillItem bi = new BillItem();
            bi.setBill(bill);
            bi.setItem(sample.getItem());
            bi.setQty(totalQty);
            bi.setPatientEncounter(sample.getPatientEncounter());
            bi.setSearialNo(serial++);
            bi.setCreatedAt(new Date());
            bi.setCreater(sessionController.getLoggedUser());

            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(bi);
            pbi.setItemBatch(sample.getItemBatch());
            pbi.setQty(totalQty);
            bi.setPharmaceuticalBillItem(pbi);

            billItemFacade.create(bi);

            Stock stock = findWardStock(sample.getItemBatch());
            if (stock != null && pharmacyBean.deductFromStock(stock, totalQty, pbi, sessionController.getDepartment())) {
                for (MedicationAdministrationRecord m : recs) {
                    m.setStockDeducted(true);
                    m.setStockDeductionBill(bill);
                    medicationAdministrationRecordFacade.edit(m);
                }
            } else {
                JsfUtil.addErrorMessage("Insufficient ward stock for " + sample.getItem().getName()
                        + " batch " + sample.getItemBatch().getBatchNo() + " - this line was skipped.");
                bi.setQty(0.0);
                pbi.setQty(0.0);
                billItemFacade.edit(bi);
            }

            settledItems.add(bi);
        }

        lastSettlementBill = bill;
        lastSettledItems = settledItems;
        printPreview = true;
        loadPendingRecords();
        selectedRecords = null;
        JsfUtil.addSuccessMessage("Ward stock updated.");
    }

    private Stock findWardStock(ItemBatch itemBatch) {
        String jpql = "SELECT s FROM Stock s WHERE s.itemBatch = :batch AND s.department = :dept";
        Map<String, Object> params = new HashMap<>();
        params.put("batch", itemBatch);
        params.put("dept", sessionController.getDepartment());
        return stockFacade.findFirstByJpql(jpql, params, true);
    }

    /**
     * Validates that ward stock covers EVERY aggregated line before any data
     * is written, mirroring
     * {@code WardPharmacyBhtIssueReceiveController#porterStockCoversAllLines}.
     */
    private boolean wardStockCoversAllLines(Map<String, List<MedicationAdministrationRecord>> groups) {
        Map<Long, Double> requiredByBatch = new HashMap<>();
        Map<Long, MedicationAdministrationRecord> sampleByBatch = new HashMap<>();
        for (List<MedicationAdministrationRecord> recs : groups.values()) {
            MedicationAdministrationRecord sample = recs.get(0);
            double totalQty = 0.0;
            for (MedicationAdministrationRecord m : recs) {
                totalQty += m.getQty();
            }
            Long batchId = sample.getItemBatch().getId();
            requiredByBatch.merge(batchId, totalQty, Double::sum);
            sampleByBatch.put(batchId, sample);
        }

        boolean allCovered = true;
        for (Map.Entry<Long, Double> e : requiredByBatch.entrySet()) {
            MedicationAdministrationRecord sample = sampleByBatch.get(e.getKey());
            Stock stock = findWardStock(sample.getItemBatch());
            double available = stock == null ? 0.0 : stock.getStock();
            if (available + 0.0001 < e.getValue()) {
                JsfUtil.addErrorMessage("Insufficient ward stock for " + sample.getItem().getName()
                        + " batch " + sample.getItemBatch().getBatchNo()
                        + ": need " + e.getValue() + " but only " + available + " available. Nothing was settled.");
                allCovered = false;
            }
        }
        return allCovered;
    }

    public List<MedicationAdministrationRecord> getPendingRecords() {
        if (pendingRecords == null) {
            pendingRecords = new ArrayList<>();
        }
        return pendingRecords;
    }

    public void setPendingRecords(List<MedicationAdministrationRecord> pendingRecords) {
        this.pendingRecords = pendingRecords;
    }

    public MedicationAdministrationRecord[] getSelectedRecords() {
        if (selectedRecords == null) {
            selectedRecords = new MedicationAdministrationRecord[0];
        }
        return selectedRecords;
    }

    public void setSelectedRecords(MedicationAdministrationRecord[] selectedRecords) {
        this.selectedRecords = selectedRecords;
    }

    public List<BillItem> getLastSettledItems() {
        if (lastSettledItems == null) {
            lastSettledItems = new ArrayList<>();
        }
        return lastSettledItems;
    }

    public void setLastSettledItems(List<BillItem> lastSettledItems) {
        this.lastSettledItems = lastSettledItems;
    }

    public Bill getLastSettlementBill() {
        return lastSettlementBill;
    }

    public void setLastSettlementBill(Bill lastSettlementBill) {
        this.lastSettlementBill = lastSettlementBill;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

}
