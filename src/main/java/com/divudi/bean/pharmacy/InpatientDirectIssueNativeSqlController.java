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
import com.divudi.core.data.dto.PrintBillData;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyService;
import com.divudi.service.pharmacy.InpatientDirectIssueNativeSqlService;
import com.divudi.service.pharmacy.PharmacySubstituteService;
import com.divudi.service.pharmacy.PriceMatrixNativeSqlService;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
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
    @EJB
    private PriceMatrixNativeSqlService priceMatrixNativeSqlService;
    @EJB
    private PharmacySubstituteService pharmacySubstituteService;
    @EJB
    private com.divudi.core.facade.BillItemFacade billItemFacade;
    @EJB
    private com.divudi.core.facade.InpatientPackageItemFacade inpatientPackageItemFacade;
    @EJB
    private com.divudi.core.facade.RoomFacilityChargeFacade roomFacilityChargeFacade;

    // ---- Working state ----
    private PatientEncounter patientEncounter;
    private Bill preBill;
    private PrintBillData printBill;
    private List<BillItemData> printBillItems;
    private BillItem billItem;
    private Double qty;
    private StockDTO selectedStockDto;
    private Long selectedStockId;
    private List<StockDTO> lastAutocompleteResults;
    private List<BillItemData> billItemDataList;
    private boolean billPreview = false;
    private String errorMessage = "";
    private double marginTotal = 0.0;
    private Bill sourceItemRequest;

    // ---- On-demand substitute (alternative) medicines (issue #22482) ----
    private BillItemData itemDataForSubstitution;
    private StockDTO selectedSubstituteStock;
    private List<StockDTO> substituteStocks;

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
        if (patientEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot issue medicines: nursing discharge has already been confirmed for this patient.");
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

            PrintBillData pbd = new PrintBillData();
            Department issuingDept = sessionController.getLoggedUser().getDepartment();
            pbd.setDepartmentName(issuingDept.getName());
            pbd.setDepartmentPrintingName(issuingDept.getPrintingName() != null ? issuingDept.getPrintingName() : issuingDept.getName());
            pbd.setDepartmentTelephone1(issuingDept.getTelephone1());
            if (issuingDept.getInstitution() != null) {
                pbd.setInstitutionName(issuingDept.getInstitution().getName());
                pbd.setInstitutionAddress(issuingDept.getInstitution().getAddress());
            }
            if (patientEncounter.getPatient() != null && patientEncounter.getPatient().getPerson() != null) {
                pbd.setPatientName(patientEncounter.getPatient().getPerson().getNameWithTitle());
                pbd.setPatientAgeSex(patientEncounter.getPatient().getPerson().getAgeAsShortString()
                        + " / " + patientEncounter.getPatient().getPerson().getSex().getLabel());
            }
            pbd.setBhtNo(patientEncounter.getBhtNo());
            if (patientEncounter.getCurrentPatientRoom() != null
                    && patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge() != null) {
                pbd.setRoomName(patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge().getName());
            }
            pbd.setBillNo(bill.getDeptId());
            pbd.setCreatedAt(bill.getCreatedAt());
            double tot = 0.0;
            double grossTot = 0.0;
            double marginTot = 0.0;
            double discountTot = 0.0;
            for (BillItemData bid : billItemDataList) {
                tot += Math.abs(bid.getNetValue());
                grossTot += Math.abs(bid.getGrossValue());
                marginTot += Math.abs(bid.getMarginValue());
                discountTot += Math.abs(bid.getDiscountValue());
            }
            pbd.setNetTotal(tot);
            pbd.setTotal(grossTot);
            pbd.setMargin(marginTot);
            pbd.setDiscount(discountTot);

            printBill = pbd;
            List<BillItemData> printCopy = new ArrayList<>();
            for (BillItemData src : billItemDataList) {
                BillItemData p = new BillItemData();
                p.setItemId(src.getItemId());
                p.setItemName(src.getItemName());
                p.setQty(Math.abs(src.getQty()));
                p.setRate(src.getRate());
                p.setNetRate(src.getNetRate());
                p.setNetValue(Math.abs(src.getNetValue()));
                p.setGrossValue(Math.abs(src.getGrossValue()));
                p.setMarginValue(Math.abs(src.getMarginValue()));
                p.setDiscountValue(Math.abs(src.getDiscountValue()));
                p.setDoe(src.getDoe());
                printCopy.add(p);
            }
            printBillItems = printCopy;
            clearBill();
            clearBillItem();
            billPreview = true;
            JsfUtil.addSuccessMessage("Bill settled successfully.");
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Native settle failed", e);
            // EJBException wraps the service RuntimeException — extract root cause for the user message
            Throwable cause = e;
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
            }
            String msg = cause.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = "Settle failed. Please check stock availability and try again.";
            }
            JsfUtil.addErrorMessage(msg);
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
                RoomFacilityCharge rfc = patientEncounter.getCurrentPatientRoom().getRoomFacilityCharge();
                Department dept = rfc.getDepartment();
                if (dept == null) {
                    // The shared L2 cache can hold a RoomFacilityCharge whose
                    // department association was still unset when it was first
                    // cached (e.g. the room-to-department link was added later,
                    // via a path that didn't go through this persistence unit).
                    // Force a fresh read before giving up.
                    RoomFacilityCharge fresh = roomFacilityChargeFacade.findFreshByJpql(
                            "select r from RoomFacilityCharge r where r.id = :id",
                            Collections.singletonMap("id", rfc.getId()));
                    dept = fresh != null ? fresh.getDepartment() : null;
                    if (dept != null) {
                        LOGGER.log(Level.WARNING, "RoomFacilityCharge id={0} had a stale null department in cache; "
                                + "resolved to department id={1} after a fresh read.",
                                new Object[]{rfc.getId(), dept.getId()});
                    } else {
                        LOGGER.log(Level.WARNING, "RoomFacilityCharge id={0} has no department even after a fresh, "
                                + "cache-bypassing read; this room is genuinely unmapped to a department.",
                                rfc.getId());
                    }
                }
                return dept;
            }
        } else if (matrixByIssuingDept) {
            return sessionController.getDepartment();
        }
        return sessionController.getDepartment();
    }

    private Bill buildBillHeader(Department matrixDept) {
        Bill b = preBill != null ? preBill : new PreBill();

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

        if (sourceItemRequest != null) {
            b.setReferenceBill(sourceItemRequest);
        }

        return b;
    }

    // -----------------------------------------------------------------------
    // Add item
    // -----------------------------------------------------------------------

    /**
     * Self-contained package-allocation check for the direct-issue flow
     * (Task 16d) — mirrors the allocation lookup + persisted consumption +
     * in-session consumption pattern used by the other two pharmacy issue
     * paths (Task 16b/16c's resolvePackageOverrideRate), but duplicated here
     * since this controller never receives a request-linked BillItem with
     * useful override state to reuse.
     */
    private com.divudi.core.entity.inward.InpatientPackageItem resolvePackageAllocation(Long itemId, double requestedQty) {
        if (patientEncounter == null || patientEncounter.getInpatientPackage() == null || itemId == null) {
            return null;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("pkg", patientEncounter.getInpatientPackage());
        m.put("itemId", itemId);
        m.put("type", com.divudi.core.data.inward.InpatientPackageComponentType.PHARMACY_ITEM);
        java.util.List<com.divudi.core.entity.inward.InpatientPackageItem> matches = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i"
                        + " WHERE i.retired = false"
                        + " AND i.inpatientPackage = :pkg"
                        + " AND i.item.id = :itemId"
                        + " AND i.componentType = :type",
                m);
        if (matches.isEmpty()) {
            return null;
        }
        com.divudi.core.entity.inward.InpatientPackageItem packageItem = matches.get(0);

        java.util.Map<String, Object> qm = new java.util.HashMap<>();
        qm.put("pe", patientEncounter);
        qm.put("itemId", itemId);
        Double alreadyIssued = billItemFacade.findDoubleByJpql(
                "SELECT SUM(bi.qty) FROM BillItem bi"
                        + " WHERE bi.retired = false"
                        + " AND bi.fromPackage = true"
                        + " AND bi.patientEncounter = :pe"
                        + " AND bi.item.id = :itemId",
                qm);
        double consumed = alreadyIssued != null ? alreadyIssued : 0.0;

        if (billItemDataList != null) {
            for (BillItemData existing : billItemDataList) {
                if (existing.isFromPackage() && itemId.equals(existing.getItemId())) {
                    consumed += existing.getQty();
                }
            }
        }

        if (consumed + requestedQty > packageItem.getQty()) {
            return null;
        }
        return packageItem;
    }

    public void addBillItem() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("Please select a BHT first.");
            return;
        }
        // itemId null means converter returned a bare fallback DTO (session miss) — treat as no selection
        if (selectedStockDto == null || selectedStockId == null || selectedStockDto.getItemId() == null) {
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
        if (billItemDataList != null) {
            for (BillItemData existing : billItemDataList) {
                if (selectedStockId.equals(existing.getStockId())) {
                    JsfUtil.addErrorMessage("This batch is already in the bill. Edit the quantity instead.");
                    return;
                }
            }
        }

        if (patientEncounter != null && selectedStockDto.getItemId() != null) {
            Item candidateItem = itemFacade.getReference(selectedStockDto.getItemId());
            String reorderMsg = pharmacyService.getReorderWarningMessage(patientEncounter, candidateItem);
            if (!reorderMsg.isEmpty()) {
                JsfUtil.addWarningMessage(reorderMsg);
            }
        }

        LOGGER.log(Level.INFO, "[addBillItem] selectedStockDto: id={0} itemBatchId={1} itemId={2} itemName={3} retailRate={4} stockQty={5}",
                new Object[]{selectedStockDto.getId(), selectedStockDto.getItemBatchId(),
                        selectedStockDto.getItemId(), selectedStockDto.getItemName(),
                        selectedStockDto.getRetailRate(), selectedStockDto.getStockQty()});

        // Fetch the four batch rates in a single lightweight JPQL query (no cascade)
        double[] batchRates = fetchBatchRates(selectedStockDto.getItemBatchId());
        double batchRetailRate    = batchRates[0];
        double batchPurchaseRate  = batchRates[1];
        double batchWholesaleRate = batchRates[2];
        Double batchCostRate      = batchRates[3] > 0 ? batchRates[3] : null;

        LOGGER.log(Level.INFO, "[addBillItem] batchRates: retail={0} purchase={1} wholesale={2} cost={3}",
                new Object[]{batchRetailRate, batchPurchaseRate, batchWholesaleRate, batchCostRate});

        // Resolve AMP item ID for stock history (AMPP → AMP, native service requires AMP ID)
        long ampItemId = resolveAmpItemId(selectedStockDto.getItemId());
        LOGGER.log(Level.INFO, "[addBillItem] itemId={0} ampItemId={1} selectedStockId={2}",
                new Object[]{selectedStockDto.getItemId(), ampItemId, selectedStockId});

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
        bid.setCostRate(batchCostRate != null ? batchCostRate : batchPurchaseRate);
        bid.setBatchRetailRate(batchRetailRate);
        bid.setBatchPurchaseRate(batchPurchaseRate);
        bid.setBatchWholesaleRate(batchWholesaleRate);
        bid.setBatchCostRate(batchCostRate);
        bid.setDoe(selectedStockDto.getDateOfExpire());
        bid.setDescription(selectedStockDto.getItemName());
        bid.setCreatedAt(new Date());
        bid.setCreaterId(sessionController.getLoggedUser().getId());
        bid.setCatId(null);

        // Rate / value for bill line — apply inward price matrix margin and discount
        com.divudi.core.entity.inward.InpatientPackageItem packageAllocation = resolvePackageAllocation(selectedStockDto.getItemId(), qty);
        boolean isPackageRate = packageAllocation != null;
        double packageRate = isPackageRate ? packageAllocation.getFixedPrice() / packageAllocation.getQty() : 0.0;
        double lineRetailRate = isPackageRate ? packageRate : (selectedStockDto.getRetailRate() != null ? selectedStockDto.getRetailRate() : 0.0);
        double absQty = Math.abs(qty);
        double grossValue = lineRetailRate * absQty;
        double marginRate = 0.0;
        double marginValue = 0.0;
        double discountPct = 0.0;
        double discountValue = 0.0;
        if (!isPackageRate) {
            long itemId = selectedStockDto.getItemId();
            double[] marginAndDiscount = computeMarginAndDiscount(itemId, lineRetailRate, absQty);
            marginRate = marginAndDiscount[0];
            marginValue = marginAndDiscount[1];
            discountPct = marginAndDiscount[2];
            discountValue = marginAndDiscount[3];
        }
        double netRate = lineRetailRate + marginRate - (absQty > 0 ? discountValue / absQty : 0.0);
        double netValue = grossValue + marginValue - discountValue;
        bid.setRate(lineRetailRate);
        bid.setNetRate(netRate);
        bid.setDiscountPercent(discountPct);
        bid.setDiscountValue(discountValue);
        bid.setMarginValue(marginValue);
        bid.setNetValue(-netValue);
        bid.setGrossValue(-grossValue);
        bid.setFromPackage(isPackageRate);
        if (isPackageRate) {
            bid.setOverriddenRate(packageRate);
            bid.setSourcePackageItemId(packageAllocation.getId());
        }

        if (billItemDataList == null) {
            billItemDataList = new ArrayList<>();
        }
        billItemDataList.add(bid);

        calTotal();
        clearBillItem();
        errorMessage = "";
    }

    /**
     * Computes inward price-matrix margin and discount for a bill line, given
     * the resolved item id, its retail rate, and the absolute quantity.
     * Extracted from {@link #addBillItem()} so the same lookup logic can be
     * reused by {@link #replaceSelectedSubstitute()} without duplicating the
     * price-matrix calls (issue #22482). On any lookup failure, falls back to
     * retail-rate-only pricing (all zeros) exactly as the original inline
     * try/catch in addBillItem() did.
     *
     * @return {marginRate, marginValue, discountPct, discountValue}
     */
    private double[] computeMarginAndDiscount(long itemId, double lineRetailRate, double absQty) {
        double grossValue = lineRetailRate * absQty;
        double marginRate = 0.0;
        double marginValue = 0.0;
        double discountPct = 0.0;
        double discountValue = 0.0;
        try {
            Department matrixDept = determineMatrixDepartment();
            if (matrixDept == null) matrixDept = sessionController.getDepartment();
            long matrixDeptId = matrixDept.getId();
            double marginPct = priceMatrixNativeSqlService.getInwardMarginPct(itemId, matrixDeptId, grossValue);
            if (marginPct != 0.0) {
                marginRate = (marginPct / 100.0) * lineRetailRate;
                marginValue = marginRate * absQty;
            }
            if (priceMatrixNativeSqlService.isDiscountAllowed(itemId)) {
                Long schemeId = patientEncounter.getPaymentScheme() != null ? patientEncounter.getPaymentScheme().getId() : null;
                Long admTypeId = patientEncounter.getAdmissionType() != null ? patientEncounter.getAdmissionType().getId() : null;
                String pmName = patientEncounter.getPaymentMethod() != null ? patientEncounter.getPaymentMethod().name() : null;
                discountPct = priceMatrixNativeSqlService.getInwardDiscountPct(itemId, pmName, schemeId, admTypeId, matrixDeptId);
                discountValue = (discountPct / 100.0) * grossValue;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[computeMarginAndDiscount] margin/discount lookup failed, using retail rate only", e);
        }
        return new double[]{marginRate, marginValue, discountPct, discountValue};
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
        double discountTot = 0.0;
        double marginTot = 0.0;
        if (billItemDataList != null) {
            for (BillItemData bid : billItemDataList) {
                netTot += Math.abs(bid.getNetValue());
                grossTot += Math.abs(bid.getGrossValue());
                discountTot += bid.getDiscountValue();
                marginTot += bid.getMarginValue();
            }
        }
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discountTot);
        marginTotal = marginTot;
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
                + "i.itemBatch.item.name, i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) "
                + "FROM Stock i "
                + "WHERE i.stock > :stockMin "
                + "AND i.department = :department "
                + "AND i.itemBatch.item.name LIKE :query "
                + "ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire";

        lastAutocompleteResults = (List<StockDTO>) stockFacade.findLightsByJpql(sql, parameters, TemporalType.TIMESTAMP, 20);
        return lastAutocompleteResults != null ? lastAutocompleteResults : new ArrayList<>();
    }

    /**
     * FIFO earliest-expiry stock lookup by item id, for pre-loading suggested
     * quantities from an Item/Service Request line (issue #21793 redesign) —
     * unlike completeAvailableStockOptimizedDto(), this looks up by exact item
     * id rather than a name search.
     */
    public StockDTO findEarliestExpiryStockForItem(Long itemId, double qty) {
        if (itemId == null) {
            return null;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", sessionController.getLoggedUser().getDepartment());
        parameters.put("itemId", itemId);
        parameters.put("stockMin", qty);

        parameters.put("today", new Date());

        String sql = "SELECT NEW com.divudi.core.data.dto.StockDTO("
                + "i.id, i.itemBatch.id, i.itemBatch.item.id, i.itemBatch.item.name, i.itemBatch.item.code, "
                + "i.itemBatch.item.name, i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) "
                + "FROM Stock i "
                + "WHERE i.stock >= :stockMin "
                + "AND i.department = :department "
                + "AND i.itemBatch.item.id = :itemId "
                + "AND (i.itemBatch.dateOfExpire IS NULL OR i.itemBatch.dateOfExpire >= :today) "
                + "ORDER BY i.itemBatch.dateOfExpire";

        @SuppressWarnings("unchecked")
        List<StockDTO> results = (List<StockDTO>) stockFacade.findLightsByJpql(sql, parameters, TemporalType.TIMESTAMP, 1);
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }

    /**
     * Entry point from the Item/Service Request pending queue (issue #21793
     * redesign): seeds this controller's cart with a suggested stock batch per
     * remaining inventory line, then hands control to the normal Direct Issue
     * page — the user reviews/edits and clicks the page's own Settle button.
     * Lines with no available stock are skipped (left remaining, reported back
     * to the queue) rather than blocking the whole navigation.
     */
    public String navigateToDirectIssueFromItemRequest(Bill itemRequest, List<BillItem> remainingLines) {
        resetAll();
        setPatientEncounter(itemRequest.getPatientEncounter());
        this.sourceItemRequest = itemRequest;
        for (BillItem requestLine : remainingLines) {
            if (requestLine.getQty() == null) {
                continue;
            }
            StockDTO stockDto = findEarliestExpiryStockForItem(
                    requestLine.getItem() != null ? requestLine.getItem().getId() : null,
                    requestLine.getQty());
            if (stockDto == null) {
                continue;
            }
            selectedStockDto = stockDto;
            selectedStockId = stockDto.getId();
            qty = requestLine.getQty();
            int sizeBefore = billItemDataList != null ? billItemDataList.size() : 0;
            addBillItem();
            if (billItemDataList != null && billItemDataList.size() > sizeBefore) {
                billItemDataList.get(billItemDataList.size() - 1).setSourceRequestBillItemId(requestLine.getId());
            }
        }
        return "/inward/pharmacy_bill_issue_bht?faces-redirect=true";
    }

    public void handleStockSelect(SelectEvent event) {
        try {
            StockDTO selectedDto = (StockDTO) event.getObject();
            this.selectedStockDto = selectedDto;
            this.selectedStockId = selectedDto != null ? selectedDto.getId() : null;
            LOGGER.log(Level.INFO, "[handleStockSelect] fired: id={0} itemId={1} itemName={2} retailRate={3}",
                    new Object[]{selectedDto != null ? selectedDto.getId() : null,
                            selectedDto != null ? selectedDto.getItemId() : null,
                            selectedDto != null ? selectedDto.getItemName() : null,
                            selectedDto != null ? selectedDto.getRetailRate() : null});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in handleStockSelect", e);
        }
    }

    // -----------------------------------------------------------------------
    // View by bill ID (from BillSearch dispatcher)
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public String viewByBillId(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        resetAll();
        Object[] result = nativeSqlService.loadViewDataByBillId(billId);
        if (result == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        patientEncounter = (PatientEncounter) result[0];
        printBill = (PrintBillData) result[1];
        printBillItems = (List<BillItemData>) result[2];
        billPreview = true;
        return "/inward/pharmacy_bill_issue_bht?faces-redirect=true";
    }

    // -----------------------------------------------------------------------
    // Clear / reset
    // -----------------------------------------------------------------------

    public void resetAll() {
        patientEncounter = null;
        preBill = null;
        printBill = null;
        printBillItems = null;
        billItem = null;
        qty = null;
        selectedStockDto = null;
        selectedStockId = null;
        lastAutocompleteResults = null;
        billItemDataList = null;
        billPreview = false;
        errorMessage = "";
        marginTotal = 0.0;
        sourceItemRequest = null;
    }

    private void clearBill() {
        preBill = null;
        billItemDataList = null;
        marginTotal = 0.0;
        sourceItemRequest = null;
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

    public void onEdit(BillItemData bid) {
        if (bid.getQty() <= 0) {
            bid.setQty(0);
            JsfUtil.addErrorMessage("Quantity must be greater than zero.");
            return;
        }
        double absQty = Math.abs(bid.getQty());
        double gross = absQty * bid.getRate();
        double discountVal = (bid.getDiscountPercent() / 100.0) * gross;
        // marginRate = netRate - rate + discountRate (since netRate = rate + marginRate - discountRate)
        double marginVal = (bid.getNetRate() - bid.getRate() * (1.0 - bid.getDiscountPercent() / 100.0)) * absQty;
        bid.setGrossValue(-gross);
        bid.setDiscountValue(discountVal);
        bid.setMarginValue(marginVal);
        bid.setNetValue(-absQty * bid.getNetRate());
        bid.setPbiQty(-absQty);
        calTotal();
    }

    // -----------------------------------------------------------------------
    // On-demand substitute (alternative) medicines (issue #22482)
    // -----------------------------------------------------------------------

    public void prepareSubstitute(BillItemData bid) {
        itemDataForSubstitution = bid;
        selectedSubstituteStock = null;
        substituteStocks = new ArrayList<>();
        if (bid == null || bid.getItemId() == null) {
            return;
        }
        if (bid.isFromPackage()) {
            JsfUtil.addErrorMessage("Package-priced items cannot be substituted.");
            return;
        }
        Item item = itemFacade.find(bid.getItemId());
        if (item == null) {
            return;
        }
        double requiredQty = Math.abs(bid.getQty());
        substituteStocks = pharmacySubstituteService.findSubstituteStocks(item, sessionController.getDepartment(), requiredQty);
    }

    public void replaceSelectedSubstitute() {
        if (itemDataForSubstitution == null || selectedSubstituteStock == null
                || selectedSubstituteStock.getStockId() == null) {
            JsfUtil.addErrorMessage("Please select a substitute stock.");
            return;
        }
        if (itemDataForSubstitution.isFromPackage()) {
            JsfUtil.addErrorMessage("Package-priced items cannot be substituted.");
            return;
        }

        StockDTO sub = selectedSubstituteStock;
        BillItemData bid = itemDataForSubstitution;
        double qty = Math.abs(bid.getQty());

        double batchRetailRate = sub.getRetailRate() != null ? sub.getRetailRate() : 0.0;
        double batchPurchaseRate = sub.getPurchaseRate() != null ? sub.getPurchaseRate() : 0.0;
        double batchWholesaleRate = sub.getWholesaleRate() != null ? sub.getWholesaleRate() : 0.0;
        Double batchCostRate = (sub.getCostRate() != null && sub.getCostRate() > 0) ? sub.getCostRate() : null;

        long ampItemId = resolveAmpItemId(sub.getItemId());

        bid.setItemId(sub.getItemId());
        bid.setItemName(sub.getItemName());
        bid.setAmpItemId(ampItemId);
        bid.setStockId(sub.getStockId());
        bid.setItemBatchId(sub.getItemBatchId());
        bid.setPbiQty(-Math.abs(qty));
        bid.setRetailRate(batchRetailRate);
        bid.setPurchaseRate(batchPurchaseRate);
        bid.setWholesaleRate(batchWholesaleRate);
        bid.setCostRate(batchCostRate != null ? batchCostRate : batchPurchaseRate);
        bid.setBatchRetailRate(batchRetailRate);
        bid.setBatchPurchaseRate(batchPurchaseRate);
        bid.setBatchWholesaleRate(batchWholesaleRate);
        bid.setBatchCostRate(batchCostRate);
        bid.setDoe(sub.getDateOfExpire());
        bid.setDescription(sub.getItemName());

        double grossValue = batchRetailRate * qty;
        double[] marginAndDiscount = computeMarginAndDiscount(sub.getItemId(), batchRetailRate, qty);
        double marginRate = marginAndDiscount[0];
        double marginValue = marginAndDiscount[1];
        double discountPct = marginAndDiscount[2];
        double discountValue = marginAndDiscount[3];
        double netRate = batchRetailRate + marginRate - (qty > 0 ? discountValue / qty : 0.0);
        double netValue = grossValue + marginValue - discountValue;

        bid.setRate(batchRetailRate);
        bid.setNetRate(netRate);
        bid.setDiscountPercent(discountPct);
        bid.setDiscountValue(discountValue);
        bid.setMarginValue(marginValue);
        bid.setNetValue(-netValue);
        bid.setGrossValue(-grossValue);

        calTotal();
        JsfUtil.addSuccessMessage("Stock replaced successfully.");
    }

    // -----------------------------------------------------------------------
    // Getters / setters
    // -----------------------------------------------------------------------

    public Bill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
        }
        return preBill;
    }

    public void setPreBill(Bill preBill) {
        this.preBill = preBill;
    }

    public PrintBillData getPrintBill() {
        return printBill;
    }

    public void setPrintBill(PrintBillData printBill) {
        this.printBill = printBill;
    }

    public List<BillItemData> getPrintBillItems() {
        return printBillItems;
    }

    public void setPrintBillItems(List<BillItemData> printBillItems) {
        this.printBillItems = printBillItems;
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

    public Double getPreviewRate() {
        if (selectedStockDto == null) return null;
        return selectedStockDto.getRetailRate();
    }

    public Double getPreviewNetValue() {
        if (selectedStockDto == null || qty == null) return null;
        double rate = selectedStockDto.getRetailRate() != null ? selectedStockDto.getRetailRate() : 0.0;
        return rate * Math.abs(qty);
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

    public double getMarginTotal() {
        return marginTotal;
    }

    public BillItemData getItemDataForSubstitution() {
        return itemDataForSubstitution;
    }

    public void setItemDataForSubstitution(BillItemData itemDataForSubstitution) {
        this.itemDataForSubstitution = itemDataForSubstitution;
    }

    public StockDTO getSelectedSubstituteStock() {
        return selectedSubstituteStock;
    }

    public void setSelectedSubstituteStock(StockDTO selectedSubstituteStock) {
        this.selectedSubstituteStock = selectedSubstituteStock;
    }

    public List<StockDTO> getSubstituteStocks() {
        return substituteStocks;
    }

    public void setSubstituteStocks(List<StockDTO> substituteStocks) {
        this.substituteStocks = substituteStocks;
    }

    public StockDtoConverter getStockDtoConverter() {
        return new StockDtoConverter();
    }

    public class StockDtoConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                Long id = Long.valueOf(value);
                if (selectedStockDto != null && id.equals(selectedStockDto.getId())) {
                    return selectedStockDto;
                }
                if (lastAutocompleteResults != null) {
                    for (StockDTO dto : lastAutocompleteResults) {
                        if (dto != null && id.equals(dto.getId())) {
                            return dto;
                        }
                    }
                }
                StockDTO dto = new StockDTO();
                dto.setId(id);
                return dto;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component, Object value) {
            if (value == null) return "";
            if (value instanceof StockDTO) {
                StockDTO dto = (StockDTO) value;
                return dto.getId() != null ? dto.getId().toString() : "";
            }
            return value.toString();
        }
    }
}
