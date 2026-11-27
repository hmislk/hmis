/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.core.data.lab.Priority;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.data.inward.AdmissionTypeTimeline;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.ejb.BillNumberGenerator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Writes the inward service bills of one settle in ONE container transaction
 * (#17562): all the bills, bill items, bill fees, components and the batch bill
 * commit together or not at all. {@link InwardServiceBillService} calls it and
 * puts the caller's objects back if it rolls back.
 *
 * <p>The process itself is unchanged from the one that was in
 * {@code InwardServiceBillService}; see that class for the two behaviours
 * {@code BillBhtController.settleBillSurgery()} depends on (the batch bill is
 * linked over the caller's whole collector, and it is returned but must not
 * become the caller's own batch bill). What changed is how it writes:</p>
 * <ul>
 * <li>a bill item and its fees are complete in memory before they are saved, so
 * each is one insert, not an insert and two updates;</li>
 * <li>the bill item and bill sums come from the fees in memory, not from
 * queries on the fees just saved;</li>
 * <li>the admission type history, and the margin and discount lookups with the
 * same key, are read once for the settle;</li>
 * <li>bill numbers come from counters that commit in their own transaction, so
 * a concurrent settle never reads a counter this one has not committed. A
 * rollback leaves a gap in the numbers, never a repeat.</li>
 * </ul>
 */
@Stateless
public class InwardServiceBillWriter {

    @Inject
    private BillBeanController billBean;
    @Inject
    private InwardBeanController inwardBean;
    @Inject
    private PriceMatrixController priceMatrixController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private AdmissionTypeHistoryService admissionTypeHistoryService;

    /**
     * Creates the inward service bills for the given entries and links them
     * under a new batch bill.
     *
     * @return the bills created by this call and the batch bill they sit under
     */
    public InwardServiceBillResult createServiceBills(InwardServiceBillRequest request) {
        if (request.getBillEntries() == null || request.getBillEntries().isEmpty()) {
            throw new InwardServiceBillException("There is nothing to bill.");
        }
        RunContext ctx = new RunContext(
                admissionTypeHistoryService.timelineFor(request.getPatientEncounter()),
                new InwardBeanController.DiscountRunCache());
        InwardServiceBillResult result = new InwardServiceBillResult();
        List<Bill> created = new ArrayList<>();

        if (billBean.calculateNumberOfBillsPerOrder(request.getBillEntries()) == 1) {
            BilledBill temp = new BilledBill();
            Bill b = saveBill(departmentOf(request.getBillEntries().get(0)), temp, request);
            // Collected the moment it exists, not at the end: these facades are
            // stateless, so the row is already committed and a later failure has
            // to be able to find it in order to retire it.
            created.add(b);
            request.getBillCollector().add(b);
            applyItemRequestReference(b, request.getBillEntries());

            List<BillItem> list = saveBillItems(b, request.getBillEntries(), request.getLoggedUser(), request, ctx);
            b.setBillItems(list);

            Priority highestPriority = Optional
                    .ofNullable(list)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(bi -> bi.getPriority() != null)
                    .map(BillItem::getPriority)
                    .max(Comparator.comparingInt(Priority::getLevel))
                    .orElse(Priority.NORMAL);

            b.setPriority(highestPriority);

            billBean.calculateBillItems(b, request.getBillEntries());
        } else {
            created.addAll(putToBills(request, ctx));
        }

        result.setBills(created);
        result.setBatchBill(saveBatchBill(request));
        return result;
    }

    /**
     * One bill per distinct {@code item.getDepartment()}, each carrying only
     * that department's entries.
     */
    private List<Bill> putToBills(InwardServiceBillRequest request, RunContext ctx) {
        List<Bill> created = new ArrayList<>();

        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : request.getBillEntries()) {
            billDepts.add(departmentOf(e));
        }
        for (Department d : billDepts) {
            BilledBill myBill = new BilledBill();
            saveBill(d, myBill, request);
            // See createServiceBills: collect on creation so a partial run is recoverable.
            created.add(myBill);
            request.getBillCollector().add(myBill);
            List<BillEntry> tmp = new ArrayList<>();
            for (BillEntry e : request.getBillEntries()) {
                if (d.equals(departmentOf(e))) {
                    tmp.add(e);
                }
            }
            applyItemRequestReference(myBill, tmp);
            List<BillItem> tmpBis = saveBillItems(myBill, tmp, request.getLoggedUser(), request, ctx);
            for (int i = 0; i < tmpBis.size(); i++) {
                tmpBis.get(i).setSearialNo(i);
            }
            billBean.calculateBillItems(myBill, tmp);
            myBill.setBillItems(tmpBis);
        }

        return created;
    }

    /**
     * If any of the entries being saved onto this bill originated from an
     * Item/Service Request line (issue #21793 redesign), set the bill's
     * referenceBill so the request stays traceable to the bill it produced.
     */
    private void applyItemRequestReference(Bill bill, List<BillEntry> entries) {
        for (BillEntry e : entries) {
            if (e.getSourceRequestBillItem() != null && e.getSourceRequestBillItem().getBill() != null) {
                bill.setReferenceBill(e.getSourceRequestBillItem().getBill());
                return;
            }
        }
    }

    /**
     * The room category of the patient's current room, or null when the patient
     * is not yet in a room (or the room has no facility charge / category). Used
     * as the room-category dimension of the inward service-margin matrix lookup
     * (issue #21977); null means "wildcard row only", preserving legacy behaviour.
     */
    private RoomCategory resolveCurrentRoomCategory(PatientEncounter encounter) {
        if (encounter == null
                || encounter.getCurrentPatientRoom() == null
                || encounter.getCurrentPatientRoom().getRoomFacilityCharge() == null) {
            return null;
        }
        return encounter.getCurrentPatientRoom().getRoomFacilityCharge().getRoomCategory();
    }

    /** What one settle reads once and shares between its fees. */
    private static final class RunContext {

        final AdmissionTypeTimeline timeline;
        final InwardBeanController.DiscountRunCache cache;

        RunContext(AdmissionTypeTimeline timeline, InwardBeanController.DiscountRunCache cache) {
            this.timeline = timeline;
            this.cache = cache;
        }
    }

    private static Department departmentOf(BillEntry entry) {
        Department d = entry.getBillItem().getItem().getDepartment();
        if (d == null) {
            throw new InwardServiceBillException("An item on the bill has no department, so it cannot be billed.");
        }
        return d;
    }

    /**
     * Saves one BillItem and its BillFees onto the bill. Sets
     * {@code inwardChargeType} from the item when it is null - that is what
     * gives the interim bill its per-charge-type breakdown.
     *
     * <p>The item and its fees are complete in memory before they are saved: the
     * links are set, the inward margin and discount are applied to each fee, and
     * the item's own figures are set. The item is then one insert, and each fee
     * one insert, whatever the margin did.</p>
     */
    private BillItem saveBillItem(Bill bill, BillItem billItem, BillEntry billEntry, List<BillFee> billFees,
            WebUser wu, InwardServiceBillRequest request, RunContext ctx) {

        billItem.setCreatedAt(new Date());
        billItem.setCreater(wu);
        billItem.setBill(bill);

        if (billItem.getInwardChargeType() == null && billItem.getItem() != null
                && billItem.getItem().getInwardChargeType() != null) {
            billItem.setInwardChargeType(billItem.getItem().getInwardChargeType());
        }

        if (billEntry != null && billEntry.getSourceRequestBillItem() != null) {
            billItem.setReferanceBillItem(billEntry.getSourceRequestBillItem());
        }

        for (BillFee bf : billFees) {
            inwardBean.prepareBillFee(bf, billItem, bill, wu);
            InwardServiceBillSums.appendOnce(billItem.getBillFees(), bf);
        }

        // The item's gross, net, margin, discount and vat are the sums of its fees as
        // they were when saved, before the margin below is applied; the settle always
        // took them at that point, so they are taken here, from the fees in memory.
        BillBeanController.applyBillFeeSumsToBillItem(billItem, InwardServiceBillSums.itemSums(billItem.getBillFees()));

        double staffFee = 0.0;
        double collectingCentreFee = 0.0;
        double hospitalFee = 0.0;
        double reagentFee = 0.0;
        double otherFee = 0.0;
        double marginFee = 0.0;

        AdmissionType typeAtBill = ctx.timeline.typeAt(bill.getCreatedAt());

        for (BillFee bf : billItem.getBillFees()) {
            // Automatic admission charges carry their price in the configuration,
            // so no inward margin is applied - and therefore no inward discount
            // matrix either, since setBillFeeMargin(...) applies both. The billed
            // amount is exactly the configured price. (Issue #23594)
            if (request.isApplyInwardMargin()) {
                PatientEncounter encounter = bill.getPatientEncounter();
                AdmissionType currentType = encounter != null ? encounter.getAdmissionType() : null;
                RoomCategory roomCategory = resolveCurrentRoomCategory(encounter);
                final double serviceValue = bf.getFeeUnitGrossValue() != null ? bf.getFeeUnitGrossValue() : bf.getFeeGrossValue();
                PriceMatrix priceMatrix = ctx.cache.matrix(
                        InwardBeanController.marginMatrixKey(
                                priceMatrixController.resolveInwardMatrixCategory(billItem.getItem(), request.getMatrixDepartment()),
                                serviceValue, request.getMatrixDepartment(), request.getMarginPaymentMethod(), null,
                                currentType, roomCategory),
                        () -> priceMatrixController.fetchInwardMargin(billItem, serviceValue, request.getMatrixDepartment(),
                                request.getMarginPaymentMethod(), null, currentType, roomCategory));
                inwardBean.setBillFeeMargin(bf, bf.getBillItem().getItem(), priceMatrix, encounter,
                        typeAtBill, ctx.cache, false);
            }

            if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                collectingCentreFee += bf.getFeeValue();
            } else if (bf.getFee().getFeeType() == FeeType.Staff) {
                staffFee += bf.getFeeValue();
            } else if (bf.getFee().getFeeType() == FeeType.Chemical) {
                reagentFee += bf.getFeeValue();
            } else if (bf.getFee().getFeeType() == FeeType.Additional) {
                otherFee += bf.getFeeValue();
            } else {
                hospitalFee += bf.getFeeValue();
            }

            marginFee += bf.getFeeMargin();
        }

        billItem.setHospitalFee(hospitalFee);
        billItem.setCollectingCentreFee(collectingCentreFee);
        billItem.setReagentFee(reagentFee);
        billItem.setOtherFee(otherFee);
        billItem.setStaffFee(staffFee);
        billItem.setMarginValue(marginFee);

        if (billItem.getId() == null) {
            // Cascades to the fees in its list.
            billItemFacade.create(billItem);
        } else {
            // A merge stores a copy of a fee that has no id and leaves this object
            // without one, so the loop below would insert it again: save new fees first.
            for (BillFee bf : billItem.getBillFees()) {
                if (bf.getId() == null) {
                    billFeeFacade.create(bf);
                }
            }
            billItemFacade.edit(billItem);
        }
        for (BillFee bf : billItem.getBillFees()) {
            if (bf.getId() == null) {
                billFeeFacade.create(bf);
            }
        }

        // After the item and its fees: a component points at the item, and a query run
        // while the item is still new would flush a component that points at it, and fail.
        billBean.saveBillComponent(billEntry, bill, wu);

        return billItem;
    }

    private List<BillItem> saveBillItems(Bill bill, List<BillEntry> billEntries, WebUser webUser,
            InwardServiceBillRequest request, RunContext ctx) {
        List<BillItem> list = new ArrayList<>();
        List<BillFee> allFees = new ArrayList<>();
        for (BillEntry e : billEntries) {
            BillItem billItem = saveBillItem(bill, e.getBillItem(), e, e.getLstBillFees(), webUser, request, ctx);
            billItem.setSearialNo(list.size());
            allFees.addAll(billItem.getBillFees());
            list.add(billItem);
        }

        // The bill's totals are the sums of its fees, as the database summed them.
        InwardServiceBillSums.applyToBill(bill, allFees);

        return list;
    }

    private Bill saveBill(Department bt, BilledBill temp, InwardServiceBillRequest request) {
        Date now = new Date();
        temp.setBillType(BillType.InwardBill);
        temp.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_BILL);
        temp.setIpOpOrCc("IP");
        billBean.setSurgeryData(temp, request.getSurgeryBatchBill(), SurgeryBillType.Service);

        temp.setDepartment(request.getCreatingDepartment());
        temp.setInstitution(request.getCreatingDepartment().getInstitution());
        temp.setPatient(request.getPatientEncounter().getPatient());
        temp.setFromDepartment(request.getMatrixDepartment());

        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setPatientEncounter(request.getPatientEncounter());
        temp.setPaymentScheme(request.getPaymentScheme());
        temp.setPaymentMethod(request.getBillPaymentMethod());
        temp.setReferredBy(request.getReferredBy());
        temp.setCreatedAt(now);
        temp.setBillDate(now);
        temp.setBillTime(now);
        temp.setCreater(request.getLoggedUser());

        boolean inpatientServiceBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination
                = configOptionApplicationController.getBooleanValueByKey(
                        "InpatientServiceBillNumberGenerateStrategy:FromDepartmentToDepartmentBillTypes", false);

        boolean inpatientServiceBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices
                = configOptionApplicationController.getBooleanValueByKey("OPD Bill Number Generation Strategy - Single Number for OPD and Inpatient Investigations and Services", false);

        boolean inpatientServiceBillNumberGenerateStrategyDefault
                = configOptionApplicationController.getBooleanValueByKey(
                        "InpatientServiceBillNumberGenerateStrategy:Default", false);

        String deptId;
        String insId;

        if (inpatientServiceBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartmentInNewTransaction(
                    bt, request.getLoggedDepartment(), BillTypeAtomic.INWARD_SERVICE_BILL);
            insId = deptId;
        } else if (inpatientServiceBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices) {
            List<BillTypeAtomic> opdAndInpatientBills = BillTypeAtomic.findOpdAndInpatientServiceAndInvestigationIndividualBillTypes();
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyInNewTransaction(request.getLoggedDepartment(), opdAndInpatientBills);
            insId = deptId;
        } else if (inpatientServiceBillNumberGenerateStrategyDefault) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyInNewTransaction(bt, BillTypeAtomic.INWARD_SERVICE_BILL);
            insId = deptId;
        } else {
            deptId = billNumberBean.departmentBillNumberGeneratorInNewTransaction(temp.getDepartment(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill);
            insId = billNumberBean.institutionBillNumberGeneratorInNewTransaction(temp.getInstitution(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWSER);
        }

        temp.setDeptId(deptId);
        temp.setInsId(insId);

        if (temp.getId() == null) {
            billFacade.create(temp);
        } else {
            billFacade.edit(temp);
        }

        return temp;

    }

    /**
     * Creates the INWARD_SERVICE_BATCH_BILL and links it both ways to every bill
     * in the caller's collector. Without it the batch-cancellation path
     * (INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION) has
     * nothing to work from, which is why this step is not optional.
     */
    private Bill saveBatchBill(InwardServiceBillRequest request) {
        Bill tmp = new BilledBill();
        tmp.setCreatedAt(new Date());
        tmp.setCreater(request.getLoggedUser());
        tmp.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
        tmp.setPatient(request.getPatientEncounter().getPatient());
        boolean opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices = configOptionApplicationController.getBooleanValueByKey("OpdBillNumberGenerateStrategy:SingleNumberForOpdAndInpatientInvestigationsAndServices", false);
        String batchBillId;

        if (opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices) {
            List<BillTypeAtomic> opdAndInpatientBills = BillTypeAtomic.findOpdAndInpatientServiceAndInvestigationBatchBillTypes();
            batchBillId = billNumberBean.departmentBatchBillNumberGeneratorYearlyForInpatientAndOpdServicesInNewTransaction(request.getLoggedDepartment(), opdAndInpatientBills);
        } else {
            batchBillId = billNumberBean.departmentBillNumberGeneratorYearlyInNewTransaction(request.getLoggedDepartment(), BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
        }

        tmp.setDeptId(batchBillId);
        tmp.setInsId(batchBillId);

        if (tmp.getId() == null) {
            billFacade.create(tmp);
        }
        // The bills below point at the batch bill, a row of the same table: write
        // all the rows first so no bill is inserted ahead of the row it points to.
        billFacade.flush();
        // Published before the linking below, which can throw: without this a
        // committed batch bill would never reach the caller and so could never be
        // retired. Kept off the collector on purpose - see the field's javadoc.
        request.setCreatedBatchBill(tmp);

        for (Bill b : request.getBillCollector()) {
            b.setBackwardReferenceBill(tmp);
            billFacade.edit(b);
        }

        for (Bill b : request.getBillCollector()) {
            tmp.getForwardReferenceBills().add(b);
        }

        billFacade.edit(tmp);

        return tmp;
    }
}
