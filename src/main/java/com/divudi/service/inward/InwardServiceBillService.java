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
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.ejb.BillNumberGenerator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Turns a list of {@link BillEntry} into inward service bills, bundled by
 * department and linked under one {@code INWARD_SERVICE_BATCH_BILL}.
 *
 * <p>This is the <i>Add Services / Investigations to BHT</i> settle pipeline,
 * lifted out of {@code BillBhtController} so the automatic admission charges
 * (issue #23594) run the very same process rather than a lookalike beside it.
 * Anything that produces bills a different way - one BillItem with no BillFee,
 * or a bill with no batch bill - cancels and refunds as zero, because both of
 * those paths read BillFees rather than BillItems.</p>
 *
 * <p>Two behaviours of the original are preserved deliberately, because
 * {@code BillBhtController.settleBillSurgery()} depends on them:</p>
 * <ul>
 * <li>the batch bill is linked over the caller's whole
 * {@link InwardServiceBillRequest#getBillCollector() collector} list, not just
 * the bills this call created, because {@code settleBillSurgery()} never resets
 * that list before settling;</li>
 * <li>the batch bill is returned but must not become the caller's
 * {@code batchBill} field. The original {@code saveBatchBill()} kept its bill
 * local, so the surgery path's later {@code saveEncounterComponents(...)} and
 * {@code updateBatchBill(...)} calls still operate on the <i>surgery</i> bill.</li>
 * </ul>
 *
 * <p>Known wrinkle preserved as-is: {@code calculateNumberOfBillsPerOrder()}
 * counts distinct {@code item.getTransDepartment()} while {@link #putToBills}
 * groups by {@code item.getDepartment()}. Where those differ, the count and the
 * grouping disagree. Changing it would silently alter how existing hand-added
 * service bills are split.</p>
 *
 * @author Buddhika
 */
@Named
@ApplicationScoped
public class InwardServiceBillService implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /**
     * Creates the inward service bills for the given entries and links them
     * under a new batch bill.
     *
     * @return the bills created by this call and the batch bill they sit under
     */
    public InwardServiceBillResult createServiceBills(InwardServiceBillRequest request) {
        InwardServiceBillResult result = new InwardServiceBillResult();
        List<Bill> created = new ArrayList<>();

        if (billBean.calculateNumberOfBillsPerOrder(request.getBillEntries()) == 1) {
            BilledBill temp = new BilledBill();
            Bill b = saveBill(request.getBillEntries().get(0).getBillItem().getItem().getDepartment(), temp, request);
            applyItemRequestReference(b, request.getBillEntries());

            List<BillItem> list = saveBillItems(b, request.getBillEntries(), request.getLoggedUser(), request);
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

            billFacade.edit(b);
            billBean.calculateBillItems(b, request.getBillEntries());
            created.add(b);
        } else {
            created.addAll(putToBills(request));
        }

        request.getBillCollector().addAll(created);

        result.setBills(created);
        result.setBatchBill(saveBatchBill(request));
        return result;
    }

    /**
     * One bill per distinct {@code item.getDepartment()}, each carrying only
     * that department's entries.
     */
    private List<Bill> putToBills(InwardServiceBillRequest request) {
        List<Bill> created = new ArrayList<>();

        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : request.getBillEntries()) {
            billDepts.add(e.getBillItem().getItem().getDepartment());
        }
        for (Department d : billDepts) {
            BilledBill myBill = new BilledBill();
            saveBill(d, myBill, request);
            List<BillEntry> tmp = new ArrayList<>();
            for (BillEntry e : request.getBillEntries()) {
                if (e.getBillItem().getItem().getDepartment().equals(d)) {
                    tmp.add(e);
                }
            }
            applyItemRequestReference(myBill, tmp);
            List<BillItem> tmpBis = saveBillItems(myBill, tmp, request.getLoggedUser(), request);
            for (int i = 0; i < tmpBis.size(); i++) {
                tmpBis.get(i).setSearialNo(i);
            }
            billBean.calculateBillItems(myBill, tmp);
            myBill.setBillItems(tmpBis);
            created.add(myBill);
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

    /**
     * Persists one BillItem and its BillFees onto the bill. Sets
     * {@code inwardChargeType} from the item when it is null - that is what
     * gives the interim bill its per-charge-type breakdown.
     */
    private BillItem saveBillItem(Bill bill, BillItem billItem, BillEntry billEntry, List<BillFee> billFees, WebUser wu) {

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

        if (billItem.getId() == null) {
            billItemFacade.create(billItem);
        }

        billBean.saveBillComponent(billEntry, bill, wu);

        for (BillFee bf : billFees) {
            inwardBean.saveBillFee(bf, billItem, bill, wu);
            billItem.getBillFees().add(bf);
        }

        billBean.updateBillItemByBillFee(billItem);

        return billItem;
    }

    private List<BillItem> saveBillItems(Bill bill, List<BillEntry> billEntries, WebUser webUser, InwardServiceBillRequest request) {
        List<BillItem> list = new ArrayList<>();
        for (BillEntry e : billEntries) {
            double staffFee = 0.0;
            double collectingCentreFee = 0.0;
            double hospitalFee = 0.0;
            double reagentFee = 0.0;
            double otherFee = 0.0;
            double marginFee = 0.0;

            BillItem billItem = saveBillItem(bill, e.getBillItem(), e, e.getLstBillFees(), webUser);
            billItem.setSearialNo(list.size());

            for (BillFee bf : billItem.getBillFees()) {
                // Automatic admission charges carry their price in the configuration,
                // so no inward margin is applied - and therefore no inward discount
                // matrix either, since setBillFeeMargin(...) applies both. The billed
                // amount is exactly the configured price. (Issue #23594)
                if (request.isApplyInwardMargin()) {
                    PriceMatrix priceMatrix = priceMatrixController.fetchInwardMargin(billItem, bf.getFeeUnitGrossValue() != null ? bf.getFeeUnitGrossValue() : bf.getFeeGrossValue(), request.getMatrixDepartment(), request.getMarginPaymentMethod(), null, bill.getPatientEncounter() != null ? bill.getPatientEncounter().getAdmissionType() : null, resolveCurrentRoomCategory(bill.getPatientEncounter()));
                    inwardBean.setBillFeeMargin(bf, bf.getBillItem().getItem(), priceMatrix, bill.getPatientEncounter());
                    billFeeFacade.edit(bf);
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

            billItemFacade.editAndCommit(billItem);

            list.add(billItem);

        }

        billBean.updateBillByBillFee(bill);

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
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(
                    bt, request.getLoggedDepartment(), BillTypeAtomic.INWARD_SERVICE_BILL);
            insId = deptId;
        } else if (inpatientServiceBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices) {
            List<BillTypeAtomic> opdAndInpatientBills = BillTypeAtomic.findOpdAndInpatientServiceAndInvestigationIndividualBillTypes();
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(request.getLoggedDepartment(), opdAndInpatientBills);
            insId = deptId;
        } else if (inpatientServiceBillNumberGenerateStrategyDefault) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(bt, BillTypeAtomic.INWARD_SERVICE_BILL);
            insId = deptId;
        } else {
            deptId = billNumberBean.departmentBillNumberGenerator(temp.getDepartment(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill);
            insId = billNumberBean.institutionBillNumberGenerator(temp.getInstitution(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill, BillNumberSuffix.INWSER);
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
            batchBillId = billNumberBean.departmentBatchBillNumberGeneratorYearlyForInpatientAndOpdServices(request.getLoggedDepartment(), opdAndInpatientBills);
        } else {
            batchBillId = billNumberBean.departmentBillNumberGeneratorYearly(request.getLoggedDepartment(), BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
        }

        tmp.setDeptId(batchBillId);
        tmp.setInsId(batchBillId);

        if (tmp.getId() == null) {
            billFacade.create(tmp);
        }

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
