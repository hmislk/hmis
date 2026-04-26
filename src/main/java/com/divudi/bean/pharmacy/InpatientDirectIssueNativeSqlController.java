/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.BillItemData;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyService;
import com.divudi.service.pharmacy.InpatientDirectIssueNativeSqlService;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

/**
 * Controller for the native SQL inpatient direct issue workflow.
 *
 * The settle path is fully native SQL (via InpatientDirectIssueNativeSqlService),
 * which avoids the EAGER cascade load (Stock → ItemBatch → Item) that is the
 * dominant cold-start cost in the original PharmacySaleBhtController settle path.
 *
 * The add-item / UI path reuses the same DTO-based autocomplete pattern as the
 * original controller; at settle time items are converted to BillItemData and
 * passed to the service.
 */
@Named
@SessionScoped
public class InpatientDirectIssueNativeSqlController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(InpatientDirectIssueNativeSqlController.class.getName());

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    @EJB
    private BillFacade billFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private BillNumberGenerator billNumberGenerator;
    @EJB
    private PharmacyService pharmacyService;
    @EJB
    private InpatientDirectIssueNativeSqlService nativeSqlService;

    // ---- Working state ----
    private PatientEncounter patientEncounter;
    private Bill preBill;
    private Bill printBill;
    private BillItem billItem;
    private Double qty;
    private StockDTO selectedStockDto;
    private Long selectedStockId;
    private List<StockDTO> lastAutocompleteResults;
    private List<BillItemData> billItemDataList;
    private boolean billPreview = false;
    private String errorMessage = "";

    @PostConstruct
    public void init() {
        resetAll();
    }

    // -----------------------------------------------------------------------
    // Settle
    // -----------------------------------------------------------------------

    public void settleInpatientDirectIssue() {
        if (billItemDataList == null || billItemDataList.isEmpty()) {
            JsfUtil.addErrorMessage("Please add items to the bill.");
            return;
        }
        if (patientEncounter == null || patientEncounter.getPatient() == null) {
            JsfUtil.addErrorMessage("Please select a BHT.");
            return;
        }
        if (patientEncounter.isDischarged()) {
            JsfUtil.addErrorMessage("Sorry, patient is discharged.");
            return;
        }
        if (patientEncounter.isPaymentFinalized()) {
            JsfUtil.addErrorMessage("Sorry, this BHT was already settled.");
            return;
        }
        if (!validateRoomForSettle()) {
            return;
        }

        Department matrixDept = determineMatrixDepartment();
        if (matrixDept == null) {
            JsfUtil.addErrorMessage("Cannot issue: no department resolved for this BHT.");
            return;
        }

        // Build the bill header in memory
        Bill bill = buildBillHeader(matrixDept);

        // Stamp dept/institution IDs on each item (needed by native service for aggregates)
        long deptId = sessionController.getLoggedUser().getDepartment().getId();
        long instId = sessionController.getLoggedUser().getDepartment().getInstitution().getId();
        for (BillItemData bid : billItemDataList) {
            bid.setDepartmentId(deptId);
            bid.setInstitutionId(instId);
        }

        try {
            nativeSqlService.settle(bill, billItemDataList);
            printBill = billFacade.find(bill.getId());
            clearBill();
            clearBillItem();
            billPreview = true;
            JsfUtil.addSuccessMessage("Bill settled successfully.");
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Native settle failed", e);
            JsfUtil.addErrorMessage("Failed to settle bill: " + e.getMessage());
        }
    }

    private boolean validateRoomForSettle() {
        if (patientEncounter.getAdmissionType() != null
                && patientEncounter.getAdmissionType().isRoomChargesAllowed()) {
            if (patientEncounter.getCurrentPatientRoom() == null) {
                JsfUtil.addErrorMessage("Please select a patient room.");
                return false;
            }
            if (patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge() == null) {
                JsfUtil.addErrorMessage("Please set up the room facility charge.");
                return false;
            }
        }
        return true;
    }

    private Department determineMatrixDepartment() {
        boolean matrixByAdmissionDept = configOptionApplicationController.getBooleanValueByKey(
                "Price Matrix is calculated from Inpatient Department for "
                + sessionController.getDepartment().getName(), true);
        boolean matrixByIssuingDept = configOptionApplicationController.getBooleanValueByKey(
                "Price Matrix is calculated from Issuing Department for "
                + sessionController.getDepartment().getName(), true);

        if (matrixByAdmissionDept) {
            if (patientEncounter == null) {
                return sessionController.getDepartment();
            }
            if (patientEncounter.getCurrentPatientRoom() == null) {
                return sessionController.getDepartment();
            }
            if (patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge() != null) {
                return patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
            }
        } else if (matrixByIssuingDept) {
            return sessionController.getDepartment();
        }
        return sessionController.getDepartment();
    }

    private Bill buildBillHeader(Department matrixDept) {
        Bill b = preBill != null ? preBill : new Bill();

        b.setBillType(BillType.PharmacyBhtPre);
        b.setBillTypeAtomic(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(
                sessionController.getLoggedUser().getDepartment(),
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
        b.setInsId(deptId);
        b.setDeptId(deptId);

        b.setDepartment(sessionController.getLoggedUser().getDepartment());
        b.setInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        b.setPatient(patientEncounter.getPatient());
        b.setPatientEncounter(patientEncounter);
        b.setFromDepartment(matrixDept);
        b.setFromInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
        b.setBillDate(new Date());
        b.setBillTime(new Date());
        b.setCreatedAt(Calendar.getInstance().getTime());
        b.setCreater(sessionController.getLoggedUser());

        // Totals will be updated by the service
        b.setTotal(0.0);
        b.setNetTotal(0.0);
        b.setGrantTotal(0.0);

        return b;
    }

    // -----------------------------------------------------------------------
    // Add item
    // -----------------------------------------------------------------------

    public void addBillItem() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("Please select a BHT first.");
            return;
        }
        if (selectedStockDto == null || selectedStockId == null) {
            JsfUtil.addErrorMessage("No stock selected.");
            return;
        }
        if (qty == null || qty <= 0.0) {
            errorMessage = "Quantity?";
            JsfUtil.addErrorMessage("Please enter a quantity.");
            return;
        }
        if (selectedStockDto.getDateOfExpire() != null
                && selectedStockDto.getDateOfExpire().before(CommonFunctions.getCurrentDateTime())) {
            JsfUtil.addErrorMessage("You are not allowed to select expired items.");
            return;
        }
        if (selectedStockDto.getStockQty() != null && qty > selectedStockDto.getStockQty()) {
            JsfUtil.addErrorMessage("No sufficient stock available.");
            return;
        }

        // Fetch the four batch rates in a single lightweight JPQL query (no cascade)
        double[] batchRates = fetchBatchRates(selectedStockDto.getItemBatchId());
        double batchRetailRate   = batchRates[0];
        double batchPurchaseRate = batchRates[1];
        double batchWholesaleRate = batchRates[2];
        double batchCostRate      = batchRates[3];

        // Resolve AMP item ID for stock history (AMPP → AMP, native service requires AMP ID)
        long ampItemId = resolveAmpItemId(selectedStockDto.getItemId());

        BillItemData bid = new BillItemData();
        bid.setItemId(selectedStockDto.getItemId());
        bid.setItemName(selectedStockDto.getItemName());
        bid.setAmpItemId(ampItemId);
        bid.setStockId(selectedStockId);
        bid.setItemBatchId(selectedStockDto.getItemBatchId());
        bid.setQty(qty);
        bid.setPbiQty(-Math.abs(qty));
        bid.setFreeQty(0.0);
        bid.setRetailRate(selectedStockDto.getRetailRate() != null ? selectedStockDto.getRetailRate() : 0.0);
        bid.setPurchaseRate(batchPurchaseRate);
        bid.setWholesaleRate(batchWholesaleRate);
        bid.setCostRate(batchCostRate > 0 ? batchCostRate : batchPurchaseRate);
        bid.setBatchRetailRate(batchRetailRate);
        bid.setBatchPurchaseRate(batchPurchaseRate);
        bid.setBatchWholesaleRate(batchWholesaleRate);
        bid.setBatchCostRate(batchCostRate > 0 ? batchCostRate : null);
        bid.setDoe(selectedStockDto.getDateOfExpire());
        bid.setDescription(selectedStockDto.getItemName());
        bid.setCreatedAt(new Date());
        bid.setCreaterId(sessionController.getLoggedUser().getId());
        bid.setCatId(selectedStockDto.getCategoryId());

        // Rate / value for bill line display
        double lineRetailRate = selectedStockDto.getRetailRate() != null ? selectedStockDto.getRetailRate() : 0.0;
        bid.setRate(lineRetailRate);
        bid.setNetRate(lineRetailRate);
        bid.setNetValue(-Math.abs(qty) * lineRetailRate);
        bid.setGrossValue(-Math.abs(qty) * lineRetailRate);

        if (billItemDataList == null) {
            billItemDataList = new ArrayList<>();
        }
        billItemDataList.add(bid);

        calTotal();
        clearBillItem();
        errorMessage = "";
    }

    private double[] fetchBatchRates(Long itemBatchId) {
        if (itemBatchId == null) {
            return new double[]{0, 0, 0, 0};
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", itemBatchId);
        String jpql = "SELECT ib.retailsaleRate, ib.purcahseRate, ib.wholesaleRate, COALESCE(ib.costRate, 0) "
                + "FROM ItemBatch ib WHERE ib.id = :id";
        try {
            Object[] row = (Object[]) itemBatchFacade.findLightsByJpql(jpql, params, TemporalType.DATE, 1)
                    .stream().findFirst().orElse(null);
            if (row == null) return new double[]{0, 0, 0, 0};
            return new double[]{
                toDouble(row[0]),
                toDouble(row[1]),
                toDouble(row[2]),
                toDouble(row[3])
            };
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not fetch batch rates for itemBatchId={0}", itemBatchId);
            return new double[]{0, 0, 0, 0};
        }
    }

    private long resolveAmpItemId(Long itemId) {
        if (itemId == null) return 0L;
        try {
            // JPQL query: select AMP ID only if item is an Ampp; returns empty if not Ampp.
            // Does not load the full Item entity — scalar projection only.
            Map<String, Object> params = new HashMap<>();
            params.put("id", itemId);
            List<?> result = itemFacade.findLightsByJpql(
                    "SELECT i.amp.id FROM Item i WHERE i.id = :id AND TYPE(i) = Ampp",
                    params, TemporalType.DATE, 1);
            if (result != null && !result.isEmpty() && result.get(0) != null) {
                return ((Number) result.get(0)).longValue();
            }
            return itemId;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not resolve AMP for itemId={0}: {1}",
                    new Object[]{itemId, e.getMessage()});
            return itemId;
        }
    }

    private static double toDouble(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    public void removeBillItemData(BillItemData bid) {
        if (billItemDataList != null) {
            billItemDataList.remove(bid);
        }
        calTotal();
    }

    private void calTotal() {
        if (getPreBill() == null) return;
        double netTot = 0.0;
        double grossTot = 0.0;
        if (billItemDataList != null) {
            for (BillItemData bid : billItemDataList) {
                netTot += bid.getNetValue();
                grossTot += bid.getGrossValue();
            }
        }
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
    }

    // -----------------------------------------------------------------------
    // Autocomplete
    // -----------------------------------------------------------------------

    public List<StockDTO> completeAvailableStockOptimizedDto(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            lastAutocompleteResults = new ArrayList<>();
            return lastAutocompleteResults;
        }
        qry = qry.replaceAll("[\\n\\r]", "").trim();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", sessionController.getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", "%" + qry + "%");

        String sql = "SELECT NEW com.divudi.core.data.dto.StockDTO("
                + "i.id, i.itemBatch.id, i.itemBatch.item.id, i.itemBatch.item.name, i.itemBatch.item.code, "
                + "i.itemBatch.item.vmp.name, i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) "
                + "FROM Stock i "
                + "WHERE i.stock > :stockMin "
                + "AND i.department = :department "
                + "AND i.itemBatch.item.name LIKE :query "
                + "ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire";

        lastAutocompleteResults = (List<StockDTO>) stockFacade.findLightsByJpql(sql, parameters, TemporalType.TIMESTAMP, 20);
        return lastAutocompleteResults != null ? lastAutocompleteResults : new ArrayList<>();
    }

    public void handleStockSelect(SelectEvent event) {
        try {
            StockDTO selectedDto = (StockDTO) event.getObject();
            this.selectedStockDto = selectedDto;
            this.selectedStockId = selectedDto != null ? selectedDto.getId() : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in handleStockSelect", e);
        }
    }

    // -----------------------------------------------------------------------
    // Clear / reset
    // -----------------------------------------------------------------------

    public void resetAll() {
        preBill = null;
        printBill = null;
        billItem = null;
        qty = null;
        selectedStockDto = null;
        selectedStockId = null;
        lastAutocompleteResults = null;
        billItemDataList = null;
        billPreview = false;
        errorMessage = "";
    }

    private void clearBill() {
        preBill = null;
        billItemDataList = null;
    }

    private void clearBillItem() {
        billItem = null;
        qty = null;
        selectedStockDto = null;
        selectedStockId = null;
        lastAutocompleteResults = null;
    }

    // -----------------------------------------------------------------------
    // Row editing
    // -----------------------------------------------------------------------

    public void onEdit(RowEditEvent event) {
        // Re-validate quantity on inline edit
        BillItemData bid = (BillItemData) event.getObject();
        if (bid.getQty() <= 0) {
            bid.setQty(0);
            JsfUtil.addErrorMessage("Quantity must be greater than zero.");
            return;
        }
        double lineRate = bid.getRate();
        bid.setNetValue(-Math.abs(bid.getQty()) * lineRate);
        bid.setGrossValue(-Math.abs(bid.getQty()) * lineRate);
        bid.setPbiQty(-Math.abs(bid.getQty()));
        calTotal();
    }

    // -----------------------------------------------------------------------
    // Getters / setters
    // -----------------------------------------------------------------------

    public Bill getPreBill() {
        if (preBill == null) {
            preBill = new Bill();
        }
        return preBill;
    }

    public void setPreBill(Bill preBill) {
        this.preBill = preBill;
    }

    public Bill getPrintBill() {
        return printBill;
    }

    public void setPrintBill(Bill printBill) {
        this.printBill = printBill;
    }

    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(billItem);
            billItem.setPharmaceuticalBillItem(pbi);
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public StockDTO getSelectedStockDto() {
        return selectedStockDto;
    }

    public void setSelectedStockDto(StockDTO selectedStockDto) {
        this.selectedStockDto = selectedStockDto;
    }

    public Long getSelectedStockId() {
        return selectedStockId;
    }

    public void setSelectedStockId(Long selectedStockId) {
        this.selectedStockId = selectedStockId;
    }

    public List<StockDTO> getLastAutocompleteResults() {
        return lastAutocompleteResults;
    }

    public List<BillItemData> getBillItemDataList() {
        if (billItemDataList == null) {
            billItemDataList = new ArrayList<>();
        }
        return billItemDataList;
    }

    public void setBillItemDataList(List<BillItemData> billItemDataList) {
        this.billItemDataList = billItemDataList;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public boolean isBillPreview() {
        return billPreview;
    }

    public void setBillPreview(boolean billPreview) {
        this.billPreview = billPreview;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
