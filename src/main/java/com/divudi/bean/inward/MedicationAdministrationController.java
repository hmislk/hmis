package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.MedicationAdministrationStatus;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.clinical.MedicationAdministrationRecord;
import com.divudi.core.entity.clinical.Prescription;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.facade.MedicationAdministrationRecordFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Stage 1 of #21469 (part of #21466): ward staff record point-in-time
 * medicine administration events against ward-held pharmacy stock.
 *
 * <p>Recording an administration here does NOT deduct stock - it only
 * creates a {@link MedicationAdministrationRecord} with
 * {@code stockDeducted = false}. Stock is deducted later, in bulk, by the
 * Stage 2 ward stock-settlement screen.</p>
 */
@Named
@SessionScoped
public class MedicationAdministrationController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private MedicationAdministrationRecordFacade medicationAdministrationRecordFacade;
    @EJB
    private StockFacade stockFacade;

    @Inject
    private SessionController sessionController;

    private ClinicalFindingValue administeringCfv;
    private List<Stock> availableWardStocks;
    private Stock selectedStock;
    private Double administerQty;
    private MedicationAdministrationStatus administerStatus;
    private String administerComments;

    private Map<Long, List<MedicationAdministrationRecord>> administrationHistoryByPrescription;

    private MedicationAdministrationRecord recordToRetire;
    private String retireReason;

    /**
     * Opens the "Mark as Administered" dialog for a ward medicine row,
     * pre-loading the ward department's stock for the prescribed AMP, sorted
     * first-expiry-first-out (FEFO).
     */
    public void openAdministerDialog(ClinicalFindingValue cfv) {
        administeringCfv = null;
        availableWardStocks = new ArrayList<>();
        selectedStock = null;
        administerQty = null;
        administerStatus = MedicationAdministrationStatus.GIVEN;
        administerComments = null;

        if (cfv == null || cfv.getPrescription() == null) {
            JsfUtil.addErrorMessage("No prescription selected.");
            return;
        }
        Prescription rx = cfv.getPrescription();
        if (rx.getItem() == null) {
            JsfUtil.addErrorMessage("This prescription has no item - cannot administer.");
            return;
        }
        String jpql;
        Map<String, Object> params = new HashMap<>();
        if (rx.getItem() instanceof Amp) {
            // Exact AMP prescription — filter stock by that specific product
            jpql = "SELECT s FROM Stock s WHERE s.itemBatch.item = :item AND s.department = :dept "
                    + "AND s.stock > 0 AND s.retired = false ORDER BY s.itemBatch.dateOfExpire ASC";
            params.put("item", rx.getItem());
        } else if (rx.getItem() instanceof Vmp) {
            // VMP (generic with form/dose) — find AMPs of that VMP
            jpql = "SELECT s FROM Stock s WHERE s.itemBatch.item.vmp = :vmp AND s.department = :dept "
                    + "AND s.stock > 0 AND s.retired = false ORDER BY s.itemBatch.dateOfExpire ASC";
            params.put("vmp", rx.getItem());
        } else {
            // VTM / ATM / other generic level — no direct entity link to AMP, show all ward AMP stocks;
            // nurse selects the dispensed product from the available list
            jpql = "SELECT s FROM Stock s WHERE TYPE(s.itemBatch.item) = Amp AND s.department = :dept "
                    + "AND s.stock > 0 AND s.retired = false "
                    + "ORDER BY s.itemBatch.item.name ASC, s.itemBatch.dateOfExpire ASC";
        }
        params.put("dept", sessionController.getDepartment());
        availableWardStocks = stockFacade.findByJpql(jpql, params);
        if (availableWardStocks == null) {
            availableWardStocks = new ArrayList<>();
        }
        if (availableWardStocks.isEmpty()) {
            JsfUtil.addErrorMessage("No ward stock available for " + rx.getItem().getName() + ".");
        } else {
            selectedStock = availableWardStocks.get(0);
        }

        administeringCfv = cfv;
        administerQty = 1.0;
    }

    /**
     * Confirms the dialog opened by {@link #openAdministerDialog}, creating a
     * new {@link MedicationAdministrationRecord} for the dose given/refused/held.
     * Stock is left untouched - see Stage 2 ward stock settlement.
     */
    public void recordAdministration() {
        if (administeringCfv == null || administeringCfv.getPrescription() == null) {
            JsfUtil.addErrorMessage("No prescription selected.");
            return;
        }
        if (administerStatus == null) {
            JsfUtil.addErrorMessage("Select a status.");
            return;
        }
        if (administerStatus == MedicationAdministrationStatus.GIVEN) {
            if (selectedStock == null) {
                JsfUtil.addErrorMessage("Select a batch.");
                return;
            }
            if (administerQty == null || administerQty <= 0) {
                JsfUtil.addErrorMessage("Enter a quantity greater than zero.");
                return;
            }
            if (administerQty > selectedStock.getStock()) {
                JsfUtil.addErrorMessage("Quantity exceeds available ward stock for the selected batch ("
                        + selectedStock.getStock() + ").");
                return;
            }
        }

        Prescription rx = administeringCfv.getPrescription();

        MedicationAdministrationRecord mar = new MedicationAdministrationRecord();
        mar.setPatientEncounter(administeringCfv.getEncounter());
        mar.setPrescription(rx);
        if (administerStatus == MedicationAdministrationStatus.GIVEN) {
            mar.setItemBatch(selectedStock.getItemBatch());
            mar.setQty(administerQty);
            // Store the AMP from the dispensed batch, not the (potentially VMP) prescription item
            mar.setItem(selectedStock.getItemBatch().getItem());
        } else {
            mar.setItem(rx.getItem());
        }
        mar.setDoseDescription(rx.getFormattedPrescriptionWithoutIndoorOutdoor());
        mar.setAdministeredAt(new Date());
        mar.setAdministeredBy(sessionController.getLoggedUser().getStaff());
        mar.setDepartment(sessionController.getDepartment());
        mar.setStatus(administerStatus);
        mar.setComments(administerComments);
        mar.setStockDeducted(false);
        mar.setCreater(sessionController.getLoggedUser());
        mar.setCreatedAt(new Date());

        medicationAdministrationRecordFacade.create(mar);

        administrationHistoryByPrescription = null;
        administeringCfv = null;
        selectedStock = null;
        administerQty = null;
        administerComments = null;
        JsfUtil.addSuccessMessage("Administration recorded.");
    }

    /**
     * Administration history for a prescription, newest first, for inline
     * display under each active ward medicine row. Cached per session-load of
     * the ward medicines page; cleared after recording or retiring a record.
     */
    public List<MedicationAdministrationRecord> getAdministrationHistory(Prescription prescription) {
        if (prescription == null || prescription.getId() == null) {
            return new ArrayList<>();
        }
        if (administrationHistoryByPrescription == null) {
            administrationHistoryByPrescription = new HashMap<>();
        }
        return administrationHistoryByPrescription.computeIfAbsent(prescription.getId(), id -> {
            String jpql = "SELECT m FROM MedicationAdministrationRecord m WHERE m.prescription.id = :id "
                    + "AND m.retired = false ORDER BY m.administeredAt DESC";
            Map<String, Object> params = new HashMap<>();
            params.put("id", id);
            List<MedicationAdministrationRecord> history = medicationAdministrationRecordFacade.findByJpql(jpql, params);
            return history == null ? new ArrayList<>() : history;
        });
    }

    public void resetAdministrationHistoryCache() {
        administrationHistoryByPrescription = null;
    }

    /**
     * Opens the retire-confirmation dialog for an accidentally-recorded
     * administration. Only records not yet included in a stock-deduction
     * batch can be retired.
     */
    public void openRetireDialog(MedicationAdministrationRecord record) {
        if (record != null && record.isStockDeducted()) {
            JsfUtil.addErrorMessage("This record has already been deducted from ward stock and cannot be retired.");
            recordToRetire = null;
            return;
        }
        recordToRetire = record;
        retireReason = null;
    }

    public void retireAdministration() {
        if (recordToRetire == null) {
            JsfUtil.addErrorMessage("No record selected.");
            return;
        }
        if (recordToRetire.isStockDeducted()) {
            JsfUtil.addErrorMessage("This record has already been deducted from ward stock and cannot be retired.");
            recordToRetire = null;
            return;
        }
        if (retireReason == null || retireReason.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter a reason for retiring this record.");
            return;
        }
        recordToRetire.setRetired(true);
        recordToRetire.setRetirer(sessionController.getLoggedUser());
        recordToRetire.setRetiredAt(new Date());
        recordToRetire.setRetireComments(retireReason.trim());
        medicationAdministrationRecordFacade.edit(recordToRetire);

        administrationHistoryByPrescription = null;
        recordToRetire = null;
        retireReason = null;
        JsfUtil.addSuccessMessage("Administration record retired.");
    }

    public ClinicalFindingValue getAdministeringCfv() {
        return administeringCfv;
    }

    public void setAdministeringCfv(ClinicalFindingValue administeringCfv) {
        this.administeringCfv = administeringCfv;
    }

    public List<Stock> getAvailableWardStocks() {
        if (availableWardStocks == null) {
            availableWardStocks = new ArrayList<>();
        }
        return availableWardStocks;
    }

    public void setAvailableWardStocks(List<Stock> availableWardStocks) {
        this.availableWardStocks = availableWardStocks;
    }

    public Stock getSelectedStock() {
        return selectedStock;
    }

    public void setSelectedStock(Stock selectedStock) {
        this.selectedStock = selectedStock;
    }

    public Double getAdministerQty() {
        return administerQty;
    }

    public void setAdministerQty(Double administerQty) {
        this.administerQty = administerQty;
    }

    public MedicationAdministrationStatus getAdministerStatus() {
        return administerStatus;
    }

    public void setAdministerStatus(MedicationAdministrationStatus administerStatus) {
        this.administerStatus = administerStatus;
    }

    public String getAdministerComments() {
        return administerComments;
    }

    public void setAdministerComments(String administerComments) {
        this.administerComments = administerComments;
    }

    public MedicationAdministrationStatus[] getAdministrationStatuses() {
        return MedicationAdministrationStatus.values();
    }

    public MedicationAdministrationRecord getRecordToRetire() {
        return recordToRetire;
    }

    public void setRecordToRetire(MedicationAdministrationRecord recordToRetire) {
        this.recordToRetire = recordToRetire;
    }

    public String getRetireReason() {
        return retireReason;
    }

    public void setRetireReason(String retireReason) {
        this.retireReason = retireReason;
    }

}
