/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ItemFeeManager;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionChargeItem;
import com.divudi.core.facade.AdmissionChargeItemFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Bills the configured routine charges for a newly saved admission
 * (issue #23594).
 *
 * <p>Deliberately additive: nothing here zeroes a room charge, and neither
 * {@code fromPackage} nor {@code sourcePackageItem} is set - those mark
 * package-locked pricing, which this is not.</p>
 *
 * <p>The bills themselves are produced by {@link InwardServiceBillService}, the
 * same pipeline the <i>Add Services / Investigations to BHT</i> screen runs, so
 * every downstream screen - interim bill, final bill, cancellation, refund,
 * credit company debtor reporting - handles them with no special-casing.</p>
 *
 * @author Buddhika
 */
@Named
@ApplicationScoped
public class AdmissionChargeApplicationBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(AdmissionChargeApplicationBean.class.getName());

    @Inject
    private InwardServiceBillService inwardServiceBillService;
    @Inject
    private BillBeanController billBean;
    @Inject
    private ItemFeeManager itemFeeManager;

    @EJB
    private AdmissionChargeItemFacade admissionChargeItemFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;

    /**
     * Resolves and bills the automatic admission charges for this encounter.
     *
     * <p>Idempotent: an encounter that already carries an admission charge batch
     * bill is left alone, so re-saving an admission cannot charge it twice.</p>
     *
     * @return the batch bill created, or null when nothing was charged
     */
    public Bill applyAdmissionChargesToAdmission(PatientEncounter encounter, WebUser loggedUser, Department loggedDepartment) {
        if (encounter == null || encounter.getId() == null) {
            return null;
        }
        if (encounter.getAdmissionChargeBatchBill() != null) {
            return null;
        }

        List<AdmissionChargeItem> resolved = resolveChargesForEncounter(encounter);
        if (resolved.isEmpty()) {
            return null;
        }

        List<BillEntry> entries = new ArrayList<>();
        for (AdmissionChargeItem config : resolved) {
            BillEntry entry = buildEntry(config, encounter);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return null;
        }

        InwardServiceBillRequest request = new InwardServiceBillRequest();
        request.setBillEntries(entries);
        request.setPatientEncounter(encounter);
        request.setMatrixDepartment(feeDepartment(encounter));
        request.setMarginPaymentMethod(encounter.getPaymentMethod());
        request.setBillPaymentMethod(encounter.getPaymentMethod());
        request.setPaymentScheme(encounter.getPaymentScheme());
        request.setLoggedUser(loggedUser);
        request.setLoggedDepartment(loggedDepartment);
        request.setCreatingDepartment(loggedUser.getDepartment());
        // The price comes from the configuration, so it is neither marked up by
        // the inward price matrix nor marked down by the inward discount matrix.
        request.setApplyInwardMargin(false);

        List<Bill> createdBills = new ArrayList<>();
        request.setBillCollector(createdBills);

        // The facades are stateless, so every bill this run creates is already
        // committed by the time the next one is built. A failure part-way through
        // therefore leaves live charge bills behind, and until the encounter
        // carries its batch bill they are invisible to the reversal path in
        // BhtEditController - which looks them up by that very field. Retire what
        // was actually created before letting the failure propagate.
        InwardServiceBillResult result;
        try {
            result = inwardServiceBillService.createServiceBills(request);
        } catch (RuntimeException ex) {
            retireCreatedBills(createdBills, null, loggedUser, ex);
            throw ex;
        }

        try {
            encounter.setAdmissionChargeBatchBill(result.getBatchBill());
            patientEncounterFacade.edit(encounter);
        } catch (RuntimeException ex) {
            retireCreatedBills(createdBills, result.getBatchBill(), loggedUser, ex);
            throw ex;
        }

        return result.getBatchBill();
    }

    /**
     * Retires the bills a failed run had already committed, along with their bill
     * items and bill fees, so nothing half-charged survives.
     *
     * <p>Retiring all three levels matters: the interim/final bill aggregation
     * filters {@code BillItem.retired = false} and the fee sums filter
     * {@code BillFee.retired = false}, so retiring only the parent bill would
     * leave the charge visible in the charge-type breakdown.</p>
     *
     * <p>Never throws - it runs while an exception is already in flight, and
     * masking that exception with a second one would hide the real cause.</p>
     */
    private void retireCreatedBills(List<Bill> bills, Bill batchBill, WebUser loggedUser, RuntimeException cause) {
        String reason = "Auto-retired: automatic admission charges failed - "
                + (cause != null ? cause.getMessage() : "unknown error");
        Date now = new Date();

        List<Bill> toRetire = new ArrayList<>(bills);
        if (batchBill != null) {
            toRetire.add(batchBill);
        }

        for (Bill bill : toRetire) {
            if (bill == null || bill.getId() == null) {
                continue;
            }
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("b", bill);

                for (BillFee bf : billFeeFacade.findByJpql(
                        "select bf from BillFee bf where bf.retired = false and bf.bill = :b", params)) {
                    bf.setRetired(true);
                    bf.setRetirer(loggedUser);
                    bf.setRetiredAt(now);
                    bf.setRetireComments(reason);
                    billFeeFacade.edit(bf);
                }

                for (BillItem bi : billItemFacade.findByJpql(
                        "select bi from BillItem bi where bi.retired = false and bi.bill = :b", params)) {
                    bi.setRetired(true);
                    bi.setRetirer(loggedUser);
                    bi.setRetiredAt(now);
                    bi.setRetireComments(reason);
                    billItemFacade.edit(bi);
                }

                bill.setRetired(true);
                bill.setRetirer(loggedUser);
                bill.setRetiredAt(now);
                bill.setRetireComments(reason);
                billFacade.edit(bill);
            } catch (RuntimeException cleanupFailure) {
                logger.log(Level.SEVERE,
                        "Could not retire admission charge bill " + bill.getId()
                        + " after a failed run; it needs cancelling by hand.", cleanupFailure);
            }
        }
    }

    /**
     * The charges that apply to this encounter, at most one row per configured
     * item.
     *
     * <p>Admission type is the outer filter and payment method the inner one.
     * Because step one is a filter rather than a preference, an
     * admission-type-specific row set completely replaces the {@code null} set
     * for that item - the configuration trap the management page warns about.</p>
     */
    public List<AdmissionChargeItem> resolveChargesForEncounter(PatientEncounter encounter) {
        List<AdmissionChargeItem> resolved = new ArrayList<>();
        if (encounter == null) {
            return resolved;
        }

        List<AdmissionChargeItem> all = admissionChargeItemFacade.findByJpql(
                "select a from AdmissionChargeItem a "
                + " where a.retired = false "
                + " and a.item is not null "
                + " order by a.orderNo, a.id");
        if (all == null || all.isEmpty()) {
            return resolved;
        }

        Map<Item, List<AdmissionChargeItem>> byItem = new LinkedHashMap<>();
        for (AdmissionChargeItem row : all) {
            byItem.computeIfAbsent(row.getItem(), k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<Item, List<AdmissionChargeItem>> perItem : byItem.entrySet()) {
            AdmissionChargeItem match = resolveForItem(perItem.getValue(), encounter);
            if (match != null) {
                resolved.add(match);
            }
        }
        return resolved;
    }

    private AdmissionChargeItem resolveForItem(List<AdmissionChargeItem> rows, PatientEncounter encounter) {
        List<AdmissionChargeItem> byType = new ArrayList<>();
        if (encounter.getAdmissionType() != null) {
            for (AdmissionChargeItem row : rows) {
                if (encounter.getAdmissionType().equals(row.getAdmissionType())) {
                    byType.add(row);
                }
            }
        }
        if (byType.isEmpty()) {
            for (AdmissionChargeItem row : rows) {
                if (row.getAdmissionType() == null) {
                    byType.add(row);
                }
            }
        }
        if (byType.isEmpty()) {
            return null;
        }

        List<AdmissionChargeItem> byPaymentMethod = new ArrayList<>();
        PaymentMethod encounterPaymentMethod = encounter.getPaymentMethod();
        if (encounterPaymentMethod != null) {
            for (AdmissionChargeItem row : byType) {
                if (encounterPaymentMethod == row.getPaymentMethod()) {
                    byPaymentMethod.add(row);
                }
            }
        }
        if (byPaymentMethod.isEmpty()) {
            // A null encounter payment method (older records) and any legacy value
            // that is neither Cash nor Credit both fall through to the null row,
            // which is the correct default.
            for (AdmissionChargeItem row : byType) {
                if (row.getPaymentMethod() == null) {
                    byPaymentMethod.add(row);
                }
            }
        }
        if (byPaymentMethod.isEmpty()) {
            return null;
        }
        return byPaymentMethod.get(0);
    }

    /**
     * One entry carrying one BillItem and exactly one BillFee holding the
     * configured price. The BillFee is not optional decoration: cancellation and
     * refund both read fees rather than bill items, so an item with no fee would
     * cancel and refund as zero.
     *
     * @return null when the configured item cannot produce a valid bill row
     */
    private BillEntry buildEntry(AdmissionChargeItem config, PatientEncounter encounter) {
        Item item = config.getItem();

        // putToBills() groups on item.getDepartment() and createBillFee() reads
        // the item's institution, so both are required. A row that fails this is
        // a configuration error the management page rejects; skip rather than
        // fail the whole admission.
        if (item.getDepartment() == null || item.getInstitution() == null) {
            logger.log(Level.WARNING, "Automatic admission charge skipped: item {0} has no department or institution.", item.getName());
            return null;
        }

        ItemFee fee = resolveFee(item);
        if (fee == null) {
            logger.log(Level.WARNING, "Automatic admission charge skipped: item {0} has no fee configured.", item.getName());
            return null;
        }

        double qty = (config.getQty() != null && config.getQty() > 0) ? config.getQty() : 1.0;
        double price = config.getPrice();

        BillItem billItem = new BillItem();
        billItem.setItem(item);
        billItem.setQty(qty);
        billItem.setRate(price);
        // The existing signal that this price is fixed and must not be re-derived
        // from the item's ItemFee rows.
        billItem.setOverriddenRate(price);
        billItem.setPatientEncounter(encounter);
        billItem.setInwardChargeType(item.getInwardChargeType());

        BillFee billFee = billBean.createBillFee(billItem, fee, encounter);
        billFee.setFeeUnitGrossValue(price);
        billFee.setFeeUnitValue(price);
        billFee.setFeeGrossValue(price * qty);
        billFee.setFeeValue(price * qty);
        billFee.setFeeUnitMargin(0.0);
        billFee.setFeeMargin(0.0);
        billFee.setFeeUnitDiscount(0.0);
        billFee.setFeeDiscount(0.0);
        applyFeeVat(billFee, item);

        List<BillFee> billFees = new ArrayList<>();
        billFees.add(billFee);

        BillEntry entry = new BillEntry();
        entry.setBillItem(billItem);
        entry.setLstBillFees(billFees);
        entry.setLstBillComponents(billBean.billComponentsFromBillItem(billItem));
        entry.setLstBillSessions(billBean.billSessionsfromBillItem(billItem));

        return entry;
    }

    /**
     * The ItemFee the single BillFee points at. {@code BillFee.fee} is a
     * required relationship that downstream code dereferences unguarded (fee
     * bucketing in the settle pipeline, {@code calculateBillItems}), so an item
     * with no fee at all cannot be charged. A non-Staff fee is preferred:
     * a routine hospital charge is not a professional fee, and Staff fees route
     * into the professional-payment paths.
     */
    private ItemFee resolveFee(Item item) {
        List<ItemFee> fees = itemFeeManager.fillFees(item);
        if (fees == null || fees.isEmpty()) {
            return null;
        }
        for (ItemFee f : fees) {
            if (f.getFeeType() != FeeType.Staff) {
                return f;
            }
        }
        return fees.get(0);
    }

    /** Same VAT rule as {@code BillBhtController.recalculateFeeVat(BillFee)}. */
    private void applyFeeVat(BillFee billFee, Item item) {
        if (item.isVatable() && item.getVatPercentage() > 0) {
            billFee.setFeeVat(Math.round(billFee.getFeeValue() * item.getVatPercentage() / 100 * 100.0) / 100.0);
        } else {
            billFee.setFeeVat(0.0);
        }
        billFee.setFeeVatPlusValue(billFee.getFeeValue() + billFee.getFeeVat());
    }

    /**
     * Same rule as {@code BillBhtController.feeDepartment(PatientEncounter)}:
     * the current room's facility-charge department when the patient is in a
     * room, and the encounter's own department when there is none. The fallback
     * is what lets these charges reach room-less Rapid / Temp A&amp;E admissions.
     */
    private Department feeDepartment(PatientEncounter encounter) {
        if (encounter.getCurrentPatientRoom() != null
                && encounter.getCurrentPatientRoom().getRoomFacilityCharge() != null) {
            return encounter.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment();
        }
        return encounter.getDepartment();
    }
}
